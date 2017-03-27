package ttps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ben_and_asaf_ttp.thetownproject.shared_resources.Commands;
import ben_and_asaf_ttp.thetownproject.shared_resources.DataPacket;
import ben_and_asaf_ttp.thetownproject.shared_resources.Game;
import ben_and_asaf_ttp.thetownproject.shared_resources.Player;
import ben_and_asaf_ttp.thetownproject.shared_resources.PlayerStatus;
import ben_and_asaf_ttp.thetownproject.shared_resources.Roles;
import db.DB;

public class GameThread extends Thread{
	
	private Server server;
	private ArrayList<ServerThread> clients;
	private ArrayList<PlayerAction> killers;
	private PlayerAction healer;
	private PlayerAction snitch;
	private int playersDone;
	private int playersAlive;
	private DataPacket gameData;
	private Game game;
	private boolean gameDisbanded = false;
	private boolean freeToJoin;
	
	public GameThread(final Server server, final int numPlayers){
		this.server = server;
		this.clients = new ArrayList<ServerThread>();
		this.game = new Game();
		this.game.setMaxPlayers(numPlayers);
	}

	public synchronized ArrayList<ServerThread> getClients() {
		return clients;
	}
	
	public synchronized Game getGame() {
		return game;
	}

	public synchronized boolean isFreeToJoin() {
		return freeToJoin;
	}

	public synchronized void setFreeToJoin(boolean freeToJoin) {
		this.freeToJoin = freeToJoin;
	}

	@Override
	public void run() {
		
		//used for tallying votes and actions in each phase
		this.killers = new ArrayList<PlayerAction>();
		this.game.setDay(false);
		
		//set player's status to ingame since the game started
		for(ServerThread client : getClients()){
			client.getPlayer().setStatus(PlayerStatus.INGAME);
			client.getPlayer().setAlive(true);
		}
		
		//set role bank and distribute roles to all players
		this.getGame().setRolesBank(RolesBank.getRoleBank(getGame().getMaxPlayers()));
		this.getGame().distributeRoles();
		System.out.println("GameThread - Thread started");
		
		//send to everyone the game has started
		this.gameData = new DataPacket();
		this.gameData.setCommand(Commands.READY);
		this.gameData.setPlayers(this.getGame().getPlayers());
		broadcast(this.gameData);
		
		
		//wait for 30 seconds to start the game
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//prepare for actions - the way the server knows when to start the next phase (synchronization of threads)
		this.playersDone = 0;
		this.playersAlive = getGame().getPlayers().size();
		
		//Main game loop
		while(!hasGameEnded()){
			//day phase
			if(this.getGame().isDay()){
				
				//wait for players
				try {
					
					//broadcast night phase has started
					this.gameData.setCommand(Commands.DAY);
					broadcast(this.gameData);
					
					synchronized(this){
						while(this.playersDone != this.playersAlive){
							System.out.println("Waiting for players to vote");
							wait();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				//change to night
				endDayCycle();
				this.getGame().setDay(false);
			}else{
				//night phase
				
				//wait for players
				try {
					
					//broadcast night phase has started
					this.gameData.setCommand(Commands.NIGHT);
					broadcast(this.gameData);
					
					synchronized(this){
						while(this.playersDone != this.playersAlive){
							System.out.println("Waiting for players to set actions");
							wait();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
				//change to day
				endNightCycle();
				this.getGame().setDay(true);
			}
			
			//reset player actions
			this.playersDone = 0;
			
		}
		
		//end the game
		endGame();
	}
	
	public void addPlayer(final ServerThread client){
		DataPacket dp = new DataPacket();
		client.getPlayer().setStatus(PlayerStatus.INQUEUE);
		client.setGame(this);
		dp.setCommand(Commands.OK);
		dp.setGame(getGame());
		client.transferDataPacket(dp);
		this.getClients().add(client);
		this.getGame().getPlayers().add(client.getPlayer());
		System.out.println("GameThread: " + client.getPlayer() + " has joined the game");
		dp.setCommand(Commands.PLAYER_JOINED);
		dp.setPlayer(client.getPlayer());
		dp.setPlayers(this.getGame().getPlayers());
		dp.setGame(null);
		broadcast(dp);
		if(this.getGame().getPlayers().size() == this.getGame().getMaxPlayers()){
			this.start();
		}
	}
	
	public void removePlayer(final ServerThread client){
		
		//remove the player from the list
		this.getClients().remove(client);
		this.getGame().getPlayers().remove(client.getPlayer());
		
		final DataPacket dp = new DataPacket();
		
		//for other players
		dp.setCommand(Commands.PLAYER_LEFT);
		dp.setPlayer(client.getPlayer());
		client.getPlayer().setStatus(PlayerStatus.ONLINE);
		client.setGame(null);
		
		//check if there's anyone to broadcast to
		if(this.getGame().getPlayers().size() > 0){
			broadcast(dp);
		}else{
			
			//if the game has no more players, stop and remove this game.
			//NOTE: A game will be over if there are no more citizens or killers (but there are still some players).
			//		So this situation happens only if the game was yet to start(Players in-queue).
			this.gameData = new DataPacket();
			this.gameData.setCommand(Commands.GAME_DISBANDED);
			this.endGame();
			return;
		}
		
		//check if the game is live or not (punishment only if the game is alive and player's alive)
		if(this.isAlive() && dp.getPlayer().isAlive()){
			
			//update the player's rating in the DB
			client.getPlayer().getStats().setRating(client.getPlayer().getStats().getRating() - 3);
			DB.getDB().updateStats(client.getPlayer());
			
			synchronized(this){
				this.playersAlive--;
			}
			
			if(hasGameEnded()){			
				
				//If the game ended because of players leaving and hurting the balance of the game
				this.gameDisbanded = true;
			}
		}
		
		System.out.println("GameThread: " + client.getPlayer().getUsername() + " has left the game");
	}
	
	public void broadcast(final DataPacket dp){
		System.out.println("GameThread - broadcast");
		for(ServerThread client : getClients()){
			client.transferDataPacket(dp);
		}
	}
	
	public void chatKillers(final DataPacket dp){
		System.out.println("GameThread - chatKillers");
		for(ServerThread client : getClients()){
			if(client.getPlayer().getRole() == Roles.KILLER){
				client.transferDataPacket(dp);
			}
		}
	}
	
	public void chatDead(final DataPacket dp){
		System.out.println("GameThread - chatDead");
		for(ServerThread client : getClients()){
			if(!client.getPlayer().isAlive()){
				client.transferDataPacket(dp);
			}
		}	
	}
	
	public void vote(final DataPacket dp){
		System.out.println("GameThread - vote");
		
		//used to synchronize the start() method, when all alive players are done
		synchronized(this){
			this.playersDone++;
			if(this.playersDone == this.playersAlive){
				notify();
			}
		}
	}
	
	public void notifyVote(final ServerThread st, final DataPacket dp){
		System.out.println("GameThread - notifyVote");
		final int i = (dp.getPlayer() != null) ? 
				this.getGame().getPlayers().indexOf(dp.getPlayer()) : 
					this.getGame().getPlayers().indexOf(dp.getPlayers().get(1));
		
		if(i != -1){
			final ArrayList<Player> voters = new ArrayList<Player>(); 
			voters.add(st.getPlayer());
			int votes = 0;
			
			//tally the vote and also save it for server log
			synchronized(this.killers){
				
				//if the list already has this entry, add to its' votes
				final PlayerAction pa = new PlayerAction(-1, i); //create comparable object
				if(this.killers.contains(pa)){
					if(dp.getNumber() == 1){
						
						//add a vote
						votes = ++this.killers.get(this.killers.indexOf(pa)).votes;
						this.getGame().getPlayers().get(i).setVotes(votes);
						voters.add(this.getGame().getPlayers().get(i));
						System.out.println(st.getPlayer().getUsername() + 
								" voted for " + 
								this.getGame().getPlayers().get(i).getUsername() + " votes: " + votes);
					}else if(dp.getNumber() == 2){
						
						//remove a vote
						votes = --this.killers.get(this.killers.indexOf(pa)).votes;
						this.getGame().getPlayers().get(i).setVotes(votes);
						voters.add(this.getGame().getPlayers().get(i));
						System.out.println(st.getPlayer().getUsername() + 
								" cancelled vote for " + 
								this.getGame().getPlayers().get(i).getUsername() + " votes: " + votes);
					}else{
						
						//remove from former
						final int j = this.getGame().getPlayers().indexOf(dp.getPlayers().get(0));
						if(j != -1){
							final PlayerAction paFormer = new PlayerAction(-1, j);
							votes = --this.killers.get(this.killers.indexOf(paFormer)).votes;
							this.getGame().getPlayers().get(j).setVotes(votes);
							
							
							//add to the new target
							votes = ++this.killers.get(this.killers.indexOf(pa)).votes;
							this.getGame().getPlayers().get(i).setVotes(votes);
							
							//add the new one first, the former second
							voters.add(this.getGame().getPlayers().get(i));
							voters.add(this.getGame().getPlayers().get(j));
							
							System.out.println(st.getPlayer().getUsername() + 
									" cancelled former vote for " + 
									this.getGame().getPlayers().get(j).getUsername() + " votes: " +this.getGame().getPlayers().get(j).getVotes() +
									" AND voted for " + this.getGame().getPlayers().get(i).getUsername() + " votes: " + votes);
						}
					}
				}else{
					
					//remove from former
					if(dp.getNumber() == 3){
						final int j = this.getGame().getPlayers().indexOf(dp.getPlayers().get(0));
						if(j != -1){
							final PlayerAction paFormer = new PlayerAction(-1, j);
							votes = --this.killers.get(this.killers.indexOf(paFormer)).votes;
							this.getGame().getPlayers().get(j).setVotes(votes);
							voters.add(this.getGame().getPlayers().get(j));
							System.out.println(st.getPlayer().getUsername() + 
									" cancelled former vote for " + 
									this.getGame().getPlayers().get(j).getUsername() + " votes: " + votes);
						}
					}
					
					//Make a new entry for the list of votes
					//instigator is ignored in the vote process therefore it's set to -1
					this.killers.add(new PlayerAction(-1, i));
					
					//add to the new target
					votes = ++this.killers.get(this.killers.size() -1).votes;
					this.getGame().getPlayers().get(i).setVotes(votes);
					voters.add(this.getGame().getPlayers().get(i));
					System.out.println(st.getPlayer().getUsername() + 
							" voted for " + 
							this.getGame().getPlayers().get(i).getUsername() + " votes: " + votes);
				}
			}
			
			dp.setCommand(Commands.VOTE);
			dp.setPlayers(voters);
			dp.setPlayer(null);
			this.broadcast(dp);
		}
	}
	
	public void setAction(final ServerThread client, final DataPacket dp){
		System.out.println("GameThread - setAction");
		
		// i = is the instigator
		// j = is the target
		int i = this.getClients().indexOf(client);
		int j = this.getGame().getPlayers().indexOf(dp.getPlayer());
		
		// also check if the player actually set some kind of action
		if(dp.getCommand() != null && dp.getPlayer() != null){
			switch(dp.getCommand()){
				case KILL:
					synchronized(this.killers){
						this.killers.add(new PlayerAction(i,j));	
					}
					break;
				case HEAL:
					this.healer = new PlayerAction(i,j);
					break;
				case SNITCH:
					this.snitch = new PlayerAction(i,j);
					break;
			}
		}
			
		//used to synchronize the start() method, when all alive players are done
		synchronized(this){
			this.playersDone++;
			if(this.playersDone == this.playersAlive){
				notify();
			}
		}
	}
	
	public void endDayCycle(){
		System.out.println("GameThread - endDayCycle");
		
		//in case no vote was registered
		if(killers.isEmpty()){
			return;
		}
		
		DataPacket dp = new DataPacket();
		
		//sort by votes, index 0 is most voted
		Collections.sort(this.killers, new Comparator<PlayerAction>(){
			@Override
			public int compare(final PlayerAction o1, final PlayerAction o2) {
				if(o1.votes < o2.votes){
					return 1;
				}else if(o1.votes > o2.votes){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
		//flag to check if there's a victim
		boolean victim = true;
		
		//If more than one person was voted against
		if(this.killers.size() > 1){
			
			//if someone got most voted (first and second aren't similar - which means a tie)
			if(this.killers.get(0).votes == this.killers.get(1).votes){
				victim = false;	
				dp.setCommand(Commands.VOTE_DRAW);
				this.broadcast(dp);
			}
		}
			
		//if somebody was executed as a result of a majority of vote
		if(victim){
			getClients().get(this.killers.get(0).target).getPlayer().setAlive(false);
			synchronized(this){
				this.playersAlive--;
			}
			
			//TODO: Guillotine procedure (Mercy or not, APPEAL to other players)
			
			
			//broadcast that this player was executed
			dp.setCommand(Commands.EXECUTE);
			dp.setPlayer(getClients().get(this.killers.get(0).target).getPlayer());
			this.broadcast(dp);
			System.out.println("endDayCycle: Executed Player is: " + this.getGame().getPlayers().get(this.killers.get(0).target));		
			
			dp.setCommand(Commands.REFRESH_PLAYERS);
			
			//null so it won't confuse with murder
			dp.setPlayer(null);
			
			//clear votes
			for(Player p :this.getGame().getPlayers()){
				p.setVotes(0);
			}
			
			//send the new playerList to the clients
			dp.setPlayers(this.getGame().getPlayers());
			this.broadcast(dp);
		}
		
		//clear the votes
		this.killers.clear();
		
		//clear votes
		if(!victim){
			for(Player p :this.getGame().getPlayers()){
				p.setVotes(0);
			}
		}
	}
	
	public void endNightCycle(){
		System.out.println("GameThread - endNightCycle");
		DataPacket dp = new DataPacket();
		int maxPosition = -1; //person who got targeted by the killers
		boolean healed = false; //Someone was healed or not, used for indicating an attempted murder
		
		//KILLERS
		//if killers even registered a kill action
		if(!this.killers.isEmpty()){
			int numKillers = 0;
			
			//Determine how many alive killers there are (for majority vote)
			for(Player p : this.getGame().getPlayers()){
				if(p.isAlive() && p.getRole() == Roles.KILLER){
					numKillers++;
				}
			}	
			
			// Who got killed (majority killed this person)
			int maxKills = 0;
			
			//if there's more than 1 killer
			if(numKillers != 1){
				
				//if there's a target then that means a killer called for it, so kills is set to 1
				for(int i = 0, kills = 1; i < this.killers.size(); i++){
					for(int j = 0; j < this.killers.size(); j++ ){
						if(this.killers.get(j).target == this.killers.get(i).target){
							kills++;
						}
							
						//set the max number of kills and the target's position
						if(kills > maxKills){
							maxKills = kills;
							maxPosition = this.killers.get(i).target;
						}
					}
				}
				
				//if the number of kills registered is a majority vote, kill the person
				if(maxKills > (numKillers / 2)){
					this.getGame().getPlayers().get(maxPosition).setAlive(false);
				}
			}else{
				
				//if there's only one killer
				maxPosition = this.killers.get(0).target;
				if(maxPosition != -1){
					this.getGame().getPlayers().get(maxPosition).setAlive(false);
				}
			}
		}
		
		//HEALER
		//Who got healed (was dead, and got healed by the healer)
		if(this.healer != null && this.getGame().getPlayers().get(this.healer.instigator).isAlive()){
			
			//if he's not alive then heal him
			if(!this.getGame().getPlayers().get(this.healer.target).isAlive()){
				this.getGame().getPlayers().get(this.healer.target).setAlive(true);
				healed = true;
				
				//update stats for healer
				long heals = this.getGame().getPlayers().get(this.healer.instigator).getStats().getHeals() + 1;
				this.getGame().getPlayers().get(this.healer.instigator).getStats().setHeals(heals);
				System.out.println("Add to: " + this.getGame().getPlayers().get(this.healer.instigator) + " heals: " + heals);
				
				//if the player healed is the player who was killed before, indicate it with maxPosition
				if(this.healer.target == maxPosition){
					maxPosition = -1;
				}
			}
		}
		
		//Killers made a successful kill
		if(maxPosition != -1){
			
			//update how many players are alive
			synchronized(this){
				this.playersAlive--;
			}
			
			//iterate over the killers (instigators) who killed the target and set their stats
			for(PlayerAction pa : this.killers){
				if(pa.target == maxPosition){
					long kills = this.getGame().getPlayers().get(pa.instigator).getStats().getKills() + 1;
					this.getGame().getPlayers().get(pa.instigator).getStats().setKills(kills);
					System.out.println("Add to: " + this.getGame().getPlayers().get(pa.instigator) + " kills: " + kills);
				}
			}
		}
		
		//SNITCH		
		//Give the information to the snitch (one snitch per game)
		//There's only one snitch per game, therefore if he's dead, no check is needed
		if(this.snitch != null){
			Player snitcher = this.getGame().getPlayers().get(this.snitch.instigator);
			if(snitcher != null && snitcher.isAlive()){
				
				//set the target into the datapacket and sent it to the snitch
				Player target = this.getGame().getPlayers().get(this.snitch.target);
				dp.setCommand(Commands.SNITCH);
				dp.setPlayer(target);
				this.getClients().get(this.snitch.instigator).transferDataPacket(dp);
				
				//because snitcher and target could be used somewhere else (references to original objects)
				synchronized(this){
					System.out.println("Snitch " + snitcher.getUsername() + " snitched on " + target.getUsername());
				}
			}
		}
		
		//clear players' actions
		this.killers.clear();
		this.healer = null;
		this.snitch = null;
		
		//no need to update players if nobody died
		if(maxPosition != -1){
			dp.setCommand(Commands.REFRESH_PLAYERS);
			dp.setPlayers(this.getGame().getPlayers());
			dp.setPlayer(this.getGame().getPlayers().get(maxPosition));
			this.broadcast(dp);
		}
		
		//If an attempt to murder has failed, indicate to everyone
		if(healed){
			dp.setCommand(Commands.HEAL);
			dp.setPlayer(null);
			dp.setPlayers(null);
			this.broadcast(dp);
		}
	}
	
	public boolean hasGameEnded(){
		int killers = 0;
		int citizens = 0;
		
		//check if the game was disbanded
		if(this.gameDisbanded){
			this.gameData = new DataPacket();
			this.gameData.setCommand(Commands.GAME_DISBANDED);
			return true;
		}
		
		//check who remained alive
		for(Player p : this.getGame().getPlayers()){
			if(p.isAlive()){
				if(p.getRole() == Roles.KILLER){
					killers++;
				}else{
					citizens++;
				}
			}
		}
		
		if(killers == 0){
			
			//no killers means the citizens won
			this.gameData = new DataPacket();
			this.gameData.setCommand(Commands.WIN_CITIZENS);
			for(Player p : this.getGame().getPlayers()){
				if( !(p.getRole() == Roles.KILLER) ){
					p.getStats().setWon(p.getStats().getWon() + 1);
					p.getStats().setRating(p.getStats().getRating() + 2);
				}else{
					p.getStats().setLost(p.getStats().getLost() + 1);
					p.getStats().setRating(p.getStats().getRating() + 1);
				}
			}
			return true;
		}else if (citizens <= 1){
			
			//One citizen or less, means that the killers won the game
			//one citizen against a killer(or more) is an automatic win
			this.gameData = new DataPacket();
			this.gameData.setCommand(Commands.WIN_KILLERS);
			for(Player p : this.getGame().getPlayers()){
				if(p.getRole() == Roles.KILLER){
					p.getStats().setWon(p.getStats().getWon() + 1);
					p.getStats().setRating(p.getStats().getRating() + 2);
				}else{
					p.getStats().setLost(p.getStats().getLost() + 1);
					p.getStats().setRating(p.getStats().getRating() + 1);
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	public void endGame(){
		System.out.println("GameThread: game has ended");
		
		// end indicates whether the game ended because of gameplay or because players have left (true is the latter)
		if(this.gameData.getCommand() != Commands.GAME_DISBANDED){
			
			//update each player's game history with this game and then set game to null (reference so GC will be called)
			for(ServerThread c : this.getClients()){
				
				//TODO: when game history is implemented enable this line
				//c.getPlayer().getGameHistory().add(this.getGame());
				c.setGame(null);
				c.getPlayer().setStatus(PlayerStatus.ONLINE);
			}
			
			//send the the game, the result of the game and update player's stats in the DB
			
			//TODO: when game history is implemented enable this line
			//DB.getDB().persistGames(this.getGame());
			
			DB.getDB().updateStats((Player[]) this.getGame().getPlayers().toArray(new Player[this.getGame().getPlayers().size()]));
			
			//clear DB context (which saves shallow copies and creates problems
			DB.getDB().getEm().clear();
			
			//give to all the players their updated stats
			this.gameData.setPlayers(this.getGame().getPlayers());
			
			//TODO: Game ending bug is here - DataPacket Stack Overflow - 
			//the data being broadcasted was set in the 'hasGameEnded()' method
			broadcast(this.gameData);
			System.out.println("GameThread: game ended normally");
		}else if(this.gameDisbanded){
			
			//If the game was disbanded and there are still players - Release players from the game
			for(ServerThread c : this.getClients()){
				c.setGame(null);
				c.getPlayer().setStatus(PlayerStatus.ONLINE);
			}
			broadcast(this.gameData);
			System.out.println("GameThread: game disbanded");
		}
		
		
		//remove this game from the server's game list and finalize this object
		server.removeGame(this);
		
		//prepare for garbage collector
		killers = null;
		healer = null;
		snitch = null;
		gameData = null;
	}
	
	//This class is used like a table for player actions
	private class PlayerAction{
		int instigator;
		int target;
		int votes;
		
		//Action done -> By whom -> Upon whom + votes for the day phase (multi-purpose class)
		PlayerAction(final int instigator, final int target){
			this.instigator = instigator;
			this.target = target;
			this.votes = 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			
			PlayerAction pa = (PlayerAction)obj;
			return pa.target == this.target;
		}
	}
}