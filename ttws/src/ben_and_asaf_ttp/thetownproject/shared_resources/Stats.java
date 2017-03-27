package ben_and_asaf_ttp.thetownproject.shared_resources;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.gson.annotations.Expose;

/**
 * {@code Stats} class, holds the information about each user's stats in the database
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Player
 * @since 1.0
 */
@Entity
@Table(name="Stats")
public class Stats{

	/**
	 * The ID of the statistics, unique to each {@code Player}
	 */
	@Column(nullable=false, name="StatsID")
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	@Expose
	int statsId;
	
	/**
     * <i>Statistics</i>, how many games the user has won
     */
	@Column(name="Won")
	@Expose
    private long won;
    
     /**
     * <i>Statistics</i>, how many games the user has lost
     */
	@Column(name="Lost")
	@Expose
    private long lost;
    
     /**
     * <i>Statistics</i>, how many players the user has killed
     */
	@Column(name="Kills")
	@Expose
    private long kills;
    
     /**
     * <i>Statistics</i>, how many players the user has healed
     */
	@Column(name="Heals")
	@Expose
    private long heals;
	
    /**
     * <i>Rating</i>, the player's rating, used for match-making
     */
    @Column(name="Rating")
    @Expose
    private int rating;
    
    /**
     * Constructor for the {@code Stats} class, creates an empty, initialized object
     */
    public Stats(){
    	reset();
    }
    
    /**
     * Reset the {@code Stats} to zero (or initialize an object)
     */
    public void reset(){
    	this.setWon(0);
    	this.setLost(0);
    	this.setKills(0);
    	this.setHeals(0);
    	this.setRating(0);
    }

    /**
     * Gets the ID of the stats, <b>PK</b> in the database
     * @return The ID of the stats
     */
	public int getStatsId() {
		return statsId;
	}

	/**
     * Gets how many games the user has won
     *
     * @return Games won
     */
    public long getWon() {
        return won;
    }

    /**
     * Sets how many games were won by the user
     *
     * @param won Games won
     */
    public void setWon(final long won) {
        this.won = won;
    }

    /**
     * Gets how many games the user lost
     *
     * @return Games lost
     */
    public long getLost() {
        return lost;
    }

    /**
     * Sets how many games the user lost
     *
     * @param lost Games lost
     */
    public void setLost(final long lost) {
        this.lost = lost;
    }

    /**
     * Gets Kills done by the user
     *
     * @return Kills done
     */
    public long getKills() {
        return kills;
    }

    /**
     * Sets Kills done by the user
     *
     * @param kills Kills done
     */
    public void setKills(final long kills) {
        this.kills = kills;
    }

    /**
     * Gets heals done by the user
     *
     * @return Heals done
     */
    public long getHeals() {
        return heals;
    }

    /**
     * Sets heals done by the user
     *
     * @param heales Heals done
     */
    public void setHeals(final long heales) {
        this.heals = heales;
    }
    
	/**
	 * Get the player's rating for matchmaking
	 * @return The player's rating for matchmaking
	 */
	public int getRating() {
		return rating;
	}

	/**
	 * Set the player's rating for matchmaking
	 * @param rating The player's rating for matchmaking
	 */
	public void setRating(final int rating) {
		this.rating = rating;
	}
    
    /**
     * Gets the Win / Lose ration from the player's stats, used for comparison
     * @see Player
     * @return Win / Lose ratio
     */
    public long getWinLoseRatio(){
    	if(this.getLost() != 0){
    		return this.getWon() / this.getLost();
    	}else{
    		return this.getWon();
    	}
    }
    
    @Override
    public String toString(){
    	return "Wins: " + this.getWon() + "\nLoses: " + this.getLost() + "\nWin / Lose Ratio: " + 
    	this.getWinLoseRatio() + "\nKill / Heal: " + this.getKills() + "/" + this.getHeals() +
    	"\nRating: " + this.rating;
    }
}
