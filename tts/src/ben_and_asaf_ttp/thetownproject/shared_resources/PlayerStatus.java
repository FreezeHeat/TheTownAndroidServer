package ben_and_asaf_ttp.thetownproject.shared_resources;

/**
 * {@code PlayerStatus} class, holds the information about each user's status in the server
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Player
 * @since 1.0
 */
public enum PlayerStatus{
	
	/**
	 * Indicates the {@code User} is offline
	 */
	OFFLINE,
	
	/**
	 * Indicates the {@code User} is online
	 */
	ONLINE,
	
	/**
	 * Indicates the {@code User} is in a queue for a game
	 */
	INQUEUE,
	
	/**
	 * Indicates the {@code User} is playing a game right now
	 */
	INGAME;
}
