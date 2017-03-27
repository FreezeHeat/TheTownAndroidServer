package ben_and_asaf_ttp.thetownproject.shared_resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.google.gson.annotations.Expose;

/**
 * {@code Game} class, holds the information about each game in the database
 * which includes the players currently playing the game
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Player
 * @since 1.0
 */
@Entity
@Table(name="Games")
public class Game{

	/**
	 * An auto generated ID for a game, used to identify a {@code Game} instance
	 */
	@Column(nullable=false, name="GameID")
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	@Expose
	private int gameId;
	
	/**
	 * The maximum amount of players to play the game
	 */
	@Column(name="MaxPlayers", nullable=true)
	@Expose
	private int maxPlayers;
	
	/**
	 * The role bank({@code ArrayList} format) for this game based on {@code maxPlayers}
	 * (<b>Note:</b> this role bank is given by the server)
	 * @see Roles
	 */
	@Transient
	private transient ArrayList<Roles> rolesBank;
	
	/**
	 * This {@code ArrayList} holds all the players that are in the game
	 */
	@ManyToMany(targetEntity=Player.class)
	@Expose
	private List<Player> players;
	
	/**
	 * The game's creation date
	 */
	@Column(nullable=false, name="DateCreated")
	@Temporal(TemporalType.TIMESTAMP)
	@Expose
	private Date date;
	
	/**
	 * Game phase
	 */
	@Transient
	private boolean day;

    /**
     * This constructor creates an empty <code>Game</code> with today's {@code Date}
     */
	public Game(){
		this.date = new Date();
		this.players = new ArrayList<Player>();
	}

	/**
	 * Get the maximum allowed number of players
	 * @return maximum allowed number of players
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * Set the maximum allowed number of players
	 * @param maxPlayers maximum allowed number of players to be set
	 */
	public void setMaxPlayers(final int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	/**
	 * Get the game's role bank
	 * @return the game's role bank
	 */
	public ArrayList<Roles> getRolesBank() {
		return rolesBank;
	}
	
	/**
	 * Set the game's role bank({@code ArrayList} format)
	 * @param rolesBank the game's role bank to be set
	 */
	public void setRolesBank(final ArrayList<Roles> rolesBank) {
		this.rolesBank = rolesBank;
	}
	
	/**
	 * Get a list of all the players in the game
	 * @return list of all the players in the game
	 */
	public List<Player> getPlayers() {
		return this.players;
	}

	/**
	 * Set a list of all the players in the game
	 * @param players the list of players to be set
	 */
	public void setPlayers(final List<Player> players) {
		this.players = players;
	}

	/**
	 * Get the game's unique ID
	 * @return the game's unique ID
	 */
	public int getGameId() {
		return gameId;
	}
	
	/**
	 * Get the game's creation date
	 * @return game's creation date
	 * @see Date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Get how many player slots are left in the game player's list
	 * <p><b>NOTE:</b><i> Used when a player wants to join an available game</i></p>
	 */
	public int getPlayerSlotsLeft(){
		return maxPlayers - players.size();
	}
	
	/**
	 * Returns the game's date as a {@code String}
	 * @return the game's date (String format)
	 */
	public String getDateString(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		return dateFormat.format(this.date);
	}
	
	/**
	 * Set the game's phase
	 * @param day true for day phase / false for night phase
	 */
	public void setDay(boolean day){
		this.day = day;
	}
	
	/**
	 * Check the game's phase
	 * @return true for day phase / false for night phase
	 */
	public boolean isDay(){
		return this.day;
	}
	
	/**
	 * Shuffles the roles bank, and sets randomized roles for the players
	 * @see Game#rolesBank
	 * @see Game#players
	 */
	public void distributeRoles(){
		Collections.shuffle(this.rolesBank);
		System.out.println("Game - Distributing roles:");
		int i = 0;
		for(Player p : this.players){
			p.setRole(rolesBank.get(i++));
			System.out.println(p.getUsername() + " : " + p.getRole().toString());
		}
	}
	
	@Override
	public String toString() {
		return "Game{" + "gameId=" + gameId + ", maxPlayers=" + maxPlayers + ", date=" + date + '}';
	}
}
