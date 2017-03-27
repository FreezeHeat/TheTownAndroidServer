package ben_and_asaf_ttp.thetownproject.shared_resources;

/**
 * This enum is for the roles in-game.
 * this is used by the server and client to identify and set roles in the game
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Game
 * @since 1.0
 */
public enum Roles{
	
	/**
	 * The citizen role
	 */
	CITIZEN,
	
	/**
	 * The Killer role - kills people
	 */
	KILLER,
	
	/**
	 * The Healer role - heals people
	 */
	HEALER,
	
	/**
	 * The Snitch role - finds out his target's role
	 */
	SNITCH;

	public DataPacket action(DataPacket dp){
		switch(this){
			case CITIZEN:
				dp.setCommand(Commands.IDLE);
				break;
			case KILLER:
				dp.setCommand(Commands.KILL);
				break;
			case HEALER:
				dp.setCommand(Commands.HEAL);
				break;
			case SNITCH:
				dp.setCommand(Commands.SNITCH);
				break;
		}
		return dp;
	}
}

