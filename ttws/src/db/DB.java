package db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import ben_and_asaf_ttp.thetownproject.shared_resources.Game;
import ben_and_asaf_ttp.thetownproject.shared_resources.Player;

/**
 * {@code DB} class, has methods for accessing the JPA database
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @since 1.0
 */
public class DB {
	private EntityManagerFactory emf;
	private EntityManager em;
	private static DB db = new DB();
	
	/**
	 * Upon creating a new {@code DB}, you create a connection to the database
	 */
	private DB(){
		this.emf = Persistence.createEntityManagerFactory("TTWS");
		this.em = emf.createEntityManager();
		System.out.println("DB: Created DB instance");
	}

	/**
	 * Get the {@code EntityManager} to act upon the database (persist, query etc..)
	 * @return the {@code EntityManager} of the database
	 */
	public EntityManager getEm() {
		return em;
	}
	
	/**
	 * Get the <b>Singleton</b> instance of the database
	 * @return the database reference
	 */
	public static DB getDB(){
		return db;
	}

	/**
	 * Persist a {@code List} of players to the database
	 * @param players the players to be added to the database
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Player
	 */
	public synchronized void persistPlayers(final Player... players){
		em.getTransaction().begin();
		for(Player p : players){
			em.persist(p);
		}
		em.getTransaction().commit();
		System.out.println("DB: Persisted player list");
	}
	
	/**
	 * Persist a number of games to the database
	 * @param games the games to be added to the database
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Game
	 */
	public synchronized void persistGames(final Game... games){
		em.getTransaction().begin();
		for(Game g : games){
			em.persist(g);
		}
		em.getTransaction().commit();
		System.out.println("DB: Persisted game list");
	}
	
	/**
	 * Update the stats of players that finished a game
	 * @param players a list of players whose stats will change
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Stats
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Player
	 */
	public synchronized void updateStats(final Player... players){
		em.getTransaction().begin();
		for(Player p : players){
			em.find(Player.class, p.getUsername()).setStats(p.getStats());
		}
		em.getTransaction().commit();
		System.out.println("DB: Updated stats for players");
	}
	
	/**
	 * Login to the database by providing the player's username and password
	 * @param p player's details
	 * @return the result: either the {@code Player} or null if unsuccessful
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Commands
	 */
	public synchronized Player login(final Player p){
		Player login = em.find(Player.class, p.getUsername());
		System.out.println("DB: Login procedure");
		if(p.equals(login)){
			return login;
		}else{
			return null;
		}
	}
	
	/**
	 * Register a {@code Player} into the database
	 * <p><b>NOTE:</b> duplicates are checked by username (username is PK in DB)<p>
	 * @param p the player's details to be registered to the database
	 * @return the result: either the {@code Player} or null if unsuccessful
	 */
	public synchronized Player register(final Player p){
		Player register = em.find(Player.class, p.getUsername());
		System.out.println("DB: Register procedure");
		if(register != null){
			return null;
		}else{
			em.getTransaction().begin();
			em.persist(p);
			em.getTransaction().commit();
			return em.find(Player.class, p.getUsername());
		}
	}
	
	//TODO: Need to change the orderby query after JPA configuration
	/**
	 * Get the top10 players from the database based on Wins, Kills and Heals
	 * @return the top10 players
	 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Stats
	 */
	public synchronized List<Player> getTop10Players(){
		Query q = em.createQuery(
				"SELECT p FROM Player p " +
				"ORDER BY p.stats.rating DESC, p.stats.won DESC, p.stats.kills DESC, p.stats.heals DESC");
		q.setMaxResults(10);
		System.out.println("DB: Get top10 players");
		return (List<Player>) q.getResultList();
	}
	
	/**
	 * Search a player in the database
	 * @param username the requested player
	 * @return the requested information, or null if not found
	 */
	public synchronized Player searchPlayer(final String username){
		Player p = em.find(Player.class, username);
		System.out.println("DB: Search player");
		if(p != null){
			return p;
		}else{
			return null;
		}
	}
	
	/**
	 * Add a friend to a Player's friend list
	 * @param username The player who's friend list is about to change
	 * @param friend The friend added to the Player's friend list
	 * @return The new friend list
	 */
	public synchronized List<Player> addFriend(final String username, final Player friend){
		Player p = em.find(Player.class, username);
		if(p.getFriends() == null){
			p.setFriends(new ArrayList<Player>());
		}
		em.getTransaction().begin();
		p.getFriends().add(friend);
		
		//remove friend request (not needed anymore and can cause problems in the future)
//		for(Player player : p.getFriendsRequests()){
//			if(player.getUsername().equals(friend.getUsername())){
//				p.getFriendsRequests().remove(player);
//				break;
//			}
//		}
		em.getTransaction().commit();
		System.out.println("DB: Added friend");
		return p.getFriends();
	}
	
	/**
	 * Remove a friend from a Player's friend list
	 * @param username The player who's friend list is about to change
	 * @param friend The friend removed from the Player's friend list
	 * @return The new friend list
	 */
	public synchronized List<Player> removeFriend(final String username, final String friend){
		Player p = em.find(Player.class, username);
		for(Player player : p.getFriends()){
			if(player.getUsername().equals(friend)){
				em.getTransaction().begin();
				p.getFriends().remove(player);
				em.getTransaction().commit();
				break;
			}
		}
		System.out.println("DB: Removed friend");
		return p.getFriends();
	}
	
	/**
	 * Add a friend request to a player's list of requests
	 * @param origin The sender of the request
	 * @param target The recipient for the request
	 */
	public synchronized void addFriendRequest(final Player origin, final String target){
		em.getTransaction().begin();
		Player p = em.find(Player.class, target);
		if(p.getFriendsRequests() == null){
			p.setFriendsRequests(new ArrayList<Player>());
		}
		p.getFriendsRequests().add(origin);
		em.getTransaction().commit();
		System.out.println("DB: Add friend request");
	}
	
	/**
	 * Remove a friend request from a player's list of requests
	 * @param recipient 
	 * @param sender
	 */
	public synchronized void removeFriendRequest(final String recipient, final Player sender){
		em.getTransaction().begin();
		Player p = em.find(Player.class, recipient);
		p.getFriendsRequests().remove(sender);
		em.getTransaction().commit();
		System.out.println("DB: Remove friend request");
	}
	
	/**
	 * Get the player's friend requests list
	 * @param username The target player
	 * @return The player's friend requests list
	 */
	public synchronized List<Player> getFriendRequests(final String username){
		Player p = em.find(Player.class, username);
		System.out.println("DB: Get Friend Requests");
		return p.getFriendsRequests();
	}
	
	/**
	 * Get the player's friends list
	 * @param username The target player
	 * @return The player's friends list
	 */
	public synchronized List<Player> getFriendList(final String username){
		Player p = em.find(Player.class, username);
		System.out.println("DB: Get Friends List");
		return p.getFriends();
	}
	
	public synchronized void setUserPassword(final String username, final String newPassword){
		em.getTransaction().begin();
		Player p = em.find(Player.class, username);
		p.setPassword(newPassword);
		em.getTransaction().commit();
		System.out.println("DB: Set password");
	}
	
	/**
	 * Closes the {@code EntityManagerFactory} and {@code EntityManager} which
	 * closes the connection to the database
	 */
	public void closeDBConnection(){
		System.out.println("DB: Closing DB connection");
		this.em.close();
		this.emf.close();
	}
	
	/**
	 * Customized finalize, in order to properly close the JPA connection
	 */
	@Override
	protected void finalize(){
		closeDBConnection();
	}
}
