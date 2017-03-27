package ben_and_asaf_ttp.thetownproject.shared_resources;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.gson.annotations.Expose;

/**
 * {@code Player} class, holds the information about each player in the database
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @since 1.0
 */

@Entity
@Table(name="Players")
public class Player{
	
	/**
     * The user's username
     * <p><b>Note:</b> PK in the database, so it's unique</p>
     */
	@Id
	@Column(name="Username", length=30)
	@Expose
	private String username;
    
    /**
     * The user's password
     */
	@Column(name="Password", length=30, nullable=false)
	@Expose
	private String password;
	
	/**
     * {@code Stats} that hold the statistics of the player
     * @see Stats
     */
	@OneToOne(cascade=CascadeType.ALL)
	@Expose
    private Stats stats;

    /**
     * {@code PlayerStatus} holds information about the player's status in the server
     * @see PlayerStatus
     */
    @Transient
    @Expose
    private PlayerStatus status;
    
    /**
     * {@code Role} holds information about the player's role in the game
     * @see Roles
     */
    @Transient
    @Expose
    private Roles role;
	
    /**
     * This list holds all the games the players has played (Game history)
     * @see Game
     */
    @ManyToMany(targetEntity=Game.class, fetch=FetchType.EAGER)
    private List<Game> gameHistory;
    
    /**
     * This list holds the player's friends list
     */
    @OneToMany(fetch=FetchType.EAGER)
    @JoinTable(name="FRIENDS")
    private List<Player> friends;
    
    /**
     * This list holds the player's friend requests
     */
    @JoinTable(name="FRIEND_REQUESTS")
    private List<Player> friendsRequests;
    
    /**
     * The player's status, alive or not
     */
    @Transient
    @Expose
    private boolean alive;
    /**
     * This constructor creates an empty <code>Player</code>
     */    
    public Player(){}
    
    /**
     * Full constructor for the {@code Player}
     * @param username Player's username
     * @param password Player's password
     * @param stats Player's stats
     * @param status Player's current status
     * @param role Player's role
     * @param gamehistory Player's game history
     * @param friends Player's friend list
     * @param friendsRequests Player's friend requests list
     * @param alive Player's status in-game if alive or not
     * @see Stats
     * @see PlayerStatus
     * @see Roles
     * @see Game
     */
    public Player(
    		final String username, final String password,
    		final Stats stats, final PlayerStatus status, final Roles role, final List<Game> gamehistory,
    		final List<Player> friends, final List<Player> friendsRequests, final boolean alive){
    	this.setUsername(username);
    	this.setPassword(password);
    	this.setStats(stats);
    	this.setStatus(status);
    	this.setRole(role);
    	this.setGameHistory(gamehistory);
    	this.setFriends(friends);
    	this.setFriendsRequests(friendsRequests);
    	this.setAlive(alive);
    }
    
    /**
     * A constructor for the {@code Player}, for initializing new players,
     * since it uses only the username and password, the rest is set to their
     * default values
     * @param username the Player's username
     * @param password the Player's password
     */
    public Player(final String username, final String password){
    	this(username, password, new Stats(), PlayerStatus.OFFLINE, null, null, null, null, false);
    }
    
    /**
     * Gets the username
     *
     * @return username
     */
	public String getUsername() {
		return this.username;
	}

	 /**
     * Sets the username
     *
     * @param username Username to set
     */
	public void setUsername(final String username) {
		this.username = username;
	}

	 /**
     * Gets the password
     *
     * @return Password
     */
	public String getPassword() {
		return this.password;
	}

	 /**
     * Sets the password
     *
     * @param password Password to be set
     */
	public void setPassword(final String password) {
		this.password = password;
	}

	/**
     * Return the {@code Stats} of the player
     * @return the player's stats
     */
    public Stats getStats(){
    	return this.stats;
    }
    
    /**
     * Set the {@code Stats} of the player
     * @param stats The player's statistics
     */
	public void setStats(final Stats stats){
		this.stats = stats;
	}
	
	/**
	 * Return the player's status (such as ONLINE, OFFLINE etc..)
	 * @return the player's status
	 * @see PlayerStatus
	 */
	public PlayerStatus getStatus(){
		return this.status;
	}
	
	/**
	 * Set the player's status (such as ONLINE, OFFLINE etc..)
	 * @param status the player's status to be set
	 * @see PlayerStatus
	 */
	public void setStatus(final PlayerStatus status){
		this.status = status;
	}
	
	/**
	 * Get the player's role
	 * @return the player's role
	 * @see Roles
	 */
	public Roles getRole() {
		return this.role;
	}

	/**
	 * Set the player's role
	 * @param role the role to be set
	 * @see Roles
	 */
	public void setRole(final Roles role) {
		this.role = role;
	}

	/**
	 * Get the player's game history
	 * @return The player's game history
	 */
	public List<Game> getGameHistory() {
		return this.gameHistory;
	}
	
	/**
	 * Set the player's game history
	 * @param gameHistory The player's game history to be set
	 */
	public void setGameHistory(final List<Game> gameHistory) {
		this.gameHistory = gameHistory;
	}
	
	/**
	 * Get the player's friends list
	 * @return The player's friends list
	 */
	public List<Player> getFriends() {
		return this.friends;
	}
	
	/**
	 * Set the player's friends list
	 * @param friends The player's friends list to be set
	 */
	public void setFriends(final List<Player> friends) {
		this.friends = friends;
	}

	/**
	 * Get the player's friends request list
	 * @return The player's friends request list
	 */
	public List<Player> getFriendsRequests() {
		return friendsRequests;
	}

	/**
	 * Set the player's friends request list
	 * @param friendsRequests The player's friends request list to be set
	 */
	public void setFriendsRequests(List<Player> friendsRequests) {
		this.friendsRequests = friendsRequests;
	}

	/**
	 * Check if the player is alive
	 * @return true\false if alive
	 */
	public boolean isAlive() {
		return this.alive;
	}

	/**
	 * Set if the player is alive or not
	 * @param alive true/false player is alive/dead
	 */
	public void setAlive(final boolean alive) {
		this.alive = alive;
	}

	@Override
    public boolean equals(final Object o){
		if(o instanceof Player){
			Player p = (Player) o;
			if(this.getUsername().equals(p.getUsername())&& 
			   this.getPassword().equals(p.getPassword())){
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String friendList = "";
		if(friends != null) {
			 friendList = friendList.concat("[");
			for (Player p : friends) {
				friendList = friendList.concat(p.getUsername() + " ,");
			}
			friendList.concat("]");
		}
		return "Player [username=" + username + ", password=" + password + ",\nstats=" + stats + ",\n status=" + status
				+ ", role=" + role + ",\n gameHistory=" + gameHistory + ",\n friends=" + friendList + 
				",\n friendRequests=" /*+ friendsRequests*/ + ",\n alive=" + alive + "]";
	}
}
