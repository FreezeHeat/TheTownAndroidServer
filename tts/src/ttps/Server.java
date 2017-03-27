package ttps;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ben_and_asaf_ttp.thetownproject.shared_resources.Commands;
import ben_and_asaf_ttp.thetownproject.shared_resources.DataPacket;
import ben_and_asaf_ttp.thetownproject.shared_resources.Player;
import ben_and_asaf_ttp.thetownproject.shared_resources.PlayerStatus;
import db.DB;

/**
 * Main server class
 * <p>Involves all input and output to the server and the management of threads</p>
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Code.ServerThread The Thread used for each connection
 * @since 1.0
 */
public class Server {

    /**
     * The main server socket, used to accept connections
     */
    private ServerSocket server;
    
    /**
     * The socket used for the server connection
     */
    private Socket connection;
    
    /**
     * Singleton database reference
     * @see db.DB
     */
    private DB db = DB.getDB();
    
    /**
     * Holds all the clients which connections were accepted
     */
    private ArrayList<ServerThread> clients = new ArrayList<>();
    
    /**
     * Holds all {@code GameThread} instances, that run active games 
     */
    private ArrayList<GameThread> games = new ArrayList<>();
    
    /**
     * The listening port used for the server
     */
    private int port;
    
    /**
     * Max connections allowed to the server
     */
    private int numClients;
    
    /**
     * Sets the server's status (should server be online or not) used in the server's main loop
     */
    private boolean online = true;
    
	public synchronized ArrayList<ServerThread> getClients() {
		return clients;
	}

	public synchronized ArrayList<GameThread> getGames() {
		return games;
	}

	/**
     * Start the server, listen to clients and handle each with a thread
     *
     * @param port port to run on
     * @param numClients number of clients to accept
     */
    public void startServer(final int port, final int numClients) {
        this.port = port;
        this.numClients = numClients;
        try {
			server = new ServerSocket(this.port, this.numClients);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server: ServerSocket FAIL!");
		}
        System.out.println("Listening for a connection on port "
                + port + " limited for " + numClients + " clients");
       
        while(online){
        		
	        // listen for connections
	        try {
				listenForConnection();
	            getClients().add(new ServerThread(this, connection));
	            getClients().get(getClients().size() - 1).start();
	            System.out.println("Client count: " + getClients().size() + ", Thread count: " + Thread.activeCount());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Server: Connection Failed");
			}
        }
    }

    /**
     * Listen for a connection
     *
     * @throws IOException
     */
    private void listenForConnection() throws IOException {
        connection = server.accept();
        System.out.println("Connection established with "
                + connection.getInetAddress().getHostAddress());
    }

    /**
     * Remove a client from the list
     *
     * @param client the client to be removed
     */
    public void removeClient(final ServerThread client) {
    	
    	//Disconnect from game first
    	if(client.getGame() != null){
    		client.getGame().removePlayer(client);
    	}
    
		this.getClients().remove(client);
        System.out.println("Removed client thread - " + client.getName() + ", ThreadId - " + client.getId() + "\n" +
        		"Client count: " + getClients().size() + ", Thread count: " + Thread.activeCount());
    }

    /**
     * Login to the server
     *
     * @param details the {@code DataPacket} with the player's details
     */
    public void login(final DataPacket details) {
    	System.out.println("Server: LOGIN");
        details.setPlayer(db.login(details.getPlayer()));
        if(details.getPlayer() != null){
        	details.setCommand(Commands.LOGIN);
        	
        	//problem with GSON because of circular reference requires sending these separately
        	//Games for game history
        	//Players for friend list
    		details.setGames(details.getPlayer().getGameHistory());
    		details.setPlayers(details.getPlayer().getFriends());
        	
        	for(ServerThread client : this.getClients()){
        		if(client.getPlayer() != null && client.getPlayer().equals(details.getPlayer())){
        			System.out.println("DB Login - ALREADY CONNECTED");
        			details.setCommand(Commands.ALREADY_CONNECTED);
        			details.setPlayer(null);
        			break;
        		}
        	}
        	System.out.println("DB Login DONE - result: " + details.getCommand());
        }else{
        	details.setCommand(Commands.WRONG_DETAILS);
        	System.out.println("DB Login FAILED - result: " + details.getCommand());
        }
    }

    /**
     * Adds a user to the list of players
     *
     * @param player The thread of a player who wishes to join
     * @param numPlayers The number of players playing in the preferred game, used for match-making and creating a new game
     * , a value of 0 means the player wishes to "Quick-Join" a game 
     */
    public void addPlayer(final ServerThread player, int numPlayers) {
    	
    	//closest rating is set to 1,000,000 since it checks for the lowest distance from player's rating(closest)
    	//so a high value is needed for the check to succeed (0 is a perfect match for rating)
    	double closestRating = 1000000;
    	double averagePlayerRating = 0;
    	double distance = 0;
    	int playersLeft = 4; //should be the maximum available - 1
    	int gamePosition = -1;
    	boolean freeToJoin = (numPlayers == 5) ? false : true;
    		
		//if there are games available
		if(!this.getGames().isEmpty()){
	    	for(GameThread gt : this.getGames()){
	    		
	    		//A thread that hasn't started yet means the game didn't start yet
	    		if(!gt.isAlive()){
	    			
	    			//if player chose not to join by rank, continue to the next game in the list
	    			if(numPlayers != 0 && gt.getGame().getMaxPlayers() != numPlayers){
	    				continue;
	    			}
	    			
	    			//if there are enough slots to join the game
	    			if(gt.getGame().getPlayerSlotsLeft() > 0){
	    				
	    				//check the average rating of all players
	    				for(Player p : gt.getGame().getPlayers()){
	    					averagePlayerRating += p.getStats().getRating();
	    				}
	    				
    					averagePlayerRating /= gt.getGame().getPlayers().size();

	    				//get the distance from the player's rating and this game's average rating
    					distance = Math.abs(averagePlayerRating - player.getPlayer().getStats().getRating());
	    				
	    				//if the distance is lower than the closest, save it and the game's position
	    				if(closestRating > distance){
	    					closestRating = distance;
	    					
	    					//if it's join by rank
	    					if( (numPlayers != 0 && closestRating <= 5) ){
	    						
	    						//It's best to join the game with a high probability to start
	    						if(gt.getGame().getPlayerSlotsLeft() <= playersLeft){
	    							playersLeft = gt.getGame().getPlayerSlotsLeft();
	    							gamePosition = this.getGames().indexOf(gt);
	    						}
	    					}else if(numPlayers == 0 && 
	    							(player.getPlayer().getStats().getRating() >= averagePlayerRating) || gt.isFreeToJoin()){
	    						//if it's join by any - join only those games which are equal or below player's rating
	    						//or if the distance is no more than 5 (same as join by rank)
	    						//This makes the probability of finding a game much higher
	    						
	    						//It's best to join the game with a high probability to start
	    						if(gt.getGame().getPlayerSlotsLeft() <= playersLeft){
	    							playersLeft = gt.getGame().getPlayerSlotsLeft();
	    							gamePosition = this.getGames().indexOf(gt);
	    						}
	    					}
	    				}
	    				
	    				//reset for the next calculation
	    				averagePlayerRating = 0;
	    			}
	    		}
	    	}
	    	
	    	if(gamePosition != -1){
	    		
		    	//after finding the best match, add this player to that game
		    	this.getGames().get(gamePosition).addPlayer(player);
	    	}
		}
		
		if(gamePosition == -1){
			
			//In case the player wanted to join any, put the smallest number for a game (fastest start time)
			if(numPlayers == 0){
				numPlayers = 5;
			}
			
			//if there are no games available, create a new one and add this player to it
			this.getGames().add(new GameThread(this, numPlayers));
			if(freeToJoin){
				this.getGames().get(this.getGames().size() - 1).setFreeToJoin(true);
			}else{
				this.getGames().get(this.getGames().size() - 1).setFreeToJoin(false);
			}
			this.getGames().get(this.getGames().size() - 1).addPlayer(player);
			System.out.println("Added game thread - " + 
					this.getGames().get(this.getGames().size() - 1).getName() + ", ThreadId - " 
					+ this.getGames().get(this.getGames().size() - 1).getId() + "\n" +
	        		"Game count: " + getGames().size() + ", Thread count: " + Thread.activeCount());
			System.out.println("Server: A new game was created for " + numPlayers + " players");
		}
        System.out.println("Server: " + player.getPlayer().getUsername() + " added to list of players");
    }
    
    public boolean joinFriend(ServerThread client, Player friend){
    	for(ServerThread st : this.getClients()){
    		if(st.getPlayer().getUsername().equals(friend.getUsername())){
    			
    			//If the friend was found, the game hasn't started and there's enough room then join the game
    			if(st.getPlayer().getStatus() == PlayerStatus.INQUEUE && st.getGame().getGame().getPlayerSlotsLeft() > 0){
    				st.getGame().addPlayer(client);
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public void removeGame(final GameThread game){
		this.getGames().remove(game);
		System.out.println("Removed game thread - " + game.getName() + ", ThreadId - " + game.getId() + "\n" +
        		"Game count: " + getGames().size() + ", Thread count: " + Thread.activeCount());
    }
    
    public void refreshFriends(final List<Player> friends){
    	ArrayList<Player> list = new ArrayList<Player>(friends);
    	
    	//using two lists, one as source and the second for the purpose of optimizing search method
    	//If a player is found, remove him from the "search" list, because he was found
    	//Update the "original" list with the player's status
    	//THIS IS NOT PERFECT, but at least it's optimized better than iterating over everything again and again
    	for(ServerThread st : this.getClients()){
    		if(st.getPlayer() != null){
    			for(Iterator<Player> it = list.iterator(); it.hasNext();){
    				Player p = it.next();
	    			if(p.getUsername().equals(st.getPlayer().getUsername())){
	    				friends.get(friends.indexOf(p)).setStatus(st.getPlayer().getStatus());
	    				it.remove();
	    				if(list.isEmpty()){
	    					return;
	    				}
	    			}
	    		}
			}
    	}
    }
    
    /**
     * Finalizing the server instance
     * <p><b>NOTE:</b> Unlike any object, the finalization of the server requires
     * the Server instance to close and all those connected to it</p>
     * @see java.net.ServerSocket#close()
     */
    @Override
    protected void finalize() {
        try {
        	online = false;
        	System.out.println("Server: CLOSING SERVER");
        	this.db.closeDBConnection();
    		DataPacket dp = new DataPacket();
    		dp.setCommand(Commands.SERVER_SHUTDOWN);
    		
        	for(ServerThread client : this.getClients()){
    			client.transferDataPacket(dp);
    			client.closeCrap();
        	}
        	
        	this.getClients().clear();
            server.close();
        } catch (IOException e) {
            System.out.println("Could not close socket");
            System.exit(-1);
        }finally{
        	try {
				super.finalize();
			} catch (Throwable e) {
				e.printStackTrace();
			}
        }
    }
}
