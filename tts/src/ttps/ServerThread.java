package ttps;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ben_and_asaf_ttp.thetownproject.shared_resources.Commands;
import ben_and_asaf_ttp.thetownproject.shared_resources.DataPacket;
import ben_and_asaf_ttp.thetownproject.shared_resources.Game;
import ben_and_asaf_ttp.thetownproject.shared_resources.Player;
import ben_and_asaf_ttp.thetownproject.shared_resources.PlayerStatus;
import db.DB;

/**
 * A Thread for each client
 * <p>used for each connection from the server, involves requests to the main
 * server and manages the player's actions</p>
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see ttps.Server The main server
 * @since 1.0
 */
public class ServerThread extends Thread {

    /**
     * The thread socket connection
     */
    private Socket connection;
    
    /**
     * The output stream from the {@code Socket}
     */
    private ObjectOutputStream out;
    
    /**
     * The input stream from the {@code Socket}
     */
    private ObjectInputStream in;
    
    /**
     * The instance of the main {@code Server}, used to call for methods within
     * the main server class.
     * @see ttps.Server
     */
    private Server server;
    
    /**
     * The instance of the {@code GameThread} the player is currently in.
     * it is the managing class for handling the game (chat, actions, etc..)
     * @see ttps.GameThread
     */
    private GameThread game;
    
    /**
     * The current player that is serviced by this Thread
     * @see ben_and_asaf_ttp.thetownproject.shared_resources.Player
     */
    private Player player;
    
    /**
     * The time when the last object was read by the <code>Socket</code>
     */
    private long lastTransmit = System.currentTimeMillis();

    /**
     * Get this client's game
     */
	public synchronized GameThread getGame() {
		return this.game;
	}
	
	/**
     * Set this client's game
     */
	public synchronized void setGame(GameThread game) {
		this.game = game;
	}

	/**
     * Get this client's player details
     */
	public synchronized Player getPlayer() {
		return this.player;
	}
	
	/**
     * Set this client's player details
     */
	public synchronized void setPlayer(Player player) {
		this.player = player;
	}

	/**
     * Initiate the thread with access to the server and socket
     *
     * @param server Main server class (Multi-Threaded)
     * @param connection Socket connection
     * @throws IOException
     */
    public ServerThread(final Server server, final Socket connection) throws IOException {
        this.server = server;
        this.connection = connection;
        out = new ObjectOutputStream(connection.getOutputStream());
        out.flush();
        in = new ObjectInputStream(connection.getInputStream());
    }

    /**
     * Server logic and protocol for each client
     */
    @Override
    public void run() {
        DataPacket input = new DataPacket();
        System.out.println("Now serving client");
        
        // if the user is not active for more than 2.5 minutes, disconnect him
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
			@Override
			public void run() {
				final long transmitTime =  System.currentTimeMillis() - lastTransmit;
				final double elapsedMinutes = (transmitTime / 1000.0) / 60;
				if(elapsedMinutes >= 3){
					try {
						this.cancel();
						System.out.println("*****\n" + ServerThread.this.getName() + " player is inactive - closing\n*****");
						ServerThread.this.finalize();
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}, 300000, 300000);
        do{
            try {
                input = input.fromJson((String) in.readObject());
                lastTransmit = System.currentTimeMillis(); 
                System.out.println("\n**********\nThread: " + this.getName() + "\nInput: " + input);
                serveClient(input);
                System.out.println("Done\n**********");
            } catch (ClassNotFoundException cnfex) {
                cnfex.printStackTrace();
                break;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                break;
            }
        } while (true);

        try {
        	
            //close all connections if it's lost
        	timer.cancel();
        	finalize();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Gets the input from the user executes the correct action
     *
     * @param input
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void serveClient(final DataPacket input)
            throws IOException, ClassNotFoundException {
        switch (input.getCommand()) {
            case LOGIN:
        		login(input);
                break;
            case REGISTER:
                register(input);
                break;
            case TOP10:
                getTop10();
                break;
            case EDIT_PASSWORD:
            	DB.getDB().setUserPassword(this.getPlayer().getUsername(), input.getMessage());
            	break;
            case READY:
                System.out.println("ServerThread: " + getPlayer().getUsername() + " requests to JOIN GAME");
                if(this.game == null){
                	server.addPlayer(this, input.getNumber());
                }
                break;
            case SEND_MESSAGE:
            	input.setPlayer(this.player);
            	getGame().broadcast(input);
                break;
            case SEND_MESSAGE_KILLER:
            	input.setPlayer(this.player);
            	getGame().chatKillers(input);
                break;
            case SEND_MESSAGE_DEAD:
            	input.setPlayer(this.player);
            	getGame().chatDead(input);
                break;
            case VOTE:
            	//If it's the final vote (4)
            	if(input.getNumber() == 4){
            		getGame().vote(input);
            	}else if (input.getNumber() >= 1){
            	// if player's still in the day phase (1,2)
            		getGame().notifyVote(this, input);
            	}
                break;
            case KILL:
            case HEAL:
            case SNITCH:
            case IDLE:

                //set action based on the player's class
            	getGame().setAction(this, input);
                break;
            case DISCONNECT:
                disconnect();
                break;
            case REFRESH_FRIENDS:
            	this.player.setFriends(DB.getDB().getFriendList(this.getPlayer().getUsername()));
            	if(this.player.getFriends().size() != 0){
	            	final ArrayList<Player> friendsList = new ArrayList<Player>(this.player.getFriends());
	        		server.refreshFriends(friendsList);
	        		for(Player p : friendsList){
	        			if(p.getStatus() == null){
	        				p.setStatus(PlayerStatus.OFFLINE);
	        			}
	        		}
	            	input.setPlayers(friendsList);
            	}
            	input.setNumber(this.server.getClients().size());
            	this.transferDataPacket(input);
            	
//            	//handle friend requests as well
//            	this.getPlayer().setFriendsRequests(DB.getDB().getFriendRequests(this.getPlayer().getUsername()));
//            	if(this.getPlayer().getFriendsRequests() != null && (!this.getPlayer().getFriendsRequests().isEmpty())){
//            		input.setCommand(Commands.FRIEND_REQUEST);
//            		input.setPlayers(this.getPlayer().getFriendsRequests());
//            		this.transferDataPacket(input);
//            	}
            	break;
            case ADD_FRIEND:  
            	
            	//Update both this player's and his friend's friends list
            	input.setPlayer(DB.getDB().searchPlayer(input.getPlayer().getUsername()));
            	if(input.getPlayer() != null){
            		this.getPlayer().setFriends(DB.getDB().addFriend(this.getPlayer().getUsername(), input.getPlayer()));
            		DB.getDB().addFriend(input.getPlayer().getUsername(), this.getPlayer());
            	}else{
            		input.setCommand(Commands.WRONG_DETAILS);
            		this.transferDataPacket(input);
            	}
            	break;
            case REMOVE_FRIEND:
            	
            	//Update both this player's and his friend's friends list
            	this.getPlayer().setFriends(DB.getDB().removeFriend(this.getPlayer().getUsername(), input.getPlayer().getUsername()));
            	DB.getDB().removeFriend(input.getPlayer().getUsername(), this.getPlayer().getUsername());
            	break;
            case FRIEND_REQUEST:
            	DB.getDB().addFriendRequest(this.getPlayer(), input.getPlayer().getUsername());
            	break;
            case FRIEND_REQUEST_DECLINE:
            	DB.getDB().removeFriendRequest(this.getPlayer().getUsername(), input.getPlayer());
            	break;
            case JOIN_FRIEND:
            	
            	//In case joining was successful, the GameThread handles the rest, otherwise send unsuccessful
            	//unsuccessful means you send the same packet back
            	if(!server.joinFriend(this, input.getPlayer())){
            		this.transferDataPacket(input);
            	}
            	break;
            case SERVER_SHUTDOWN:
            	server.finalize();
            	break;
            default:
            	System.out.println("ServeClient: UNEXPECTED INPUT");
            	break;
        }
    }

    /**
     * Close all connections and remove this thread
     *
     * @throws IOException
     */
    public void closeCrap() throws IOException {
        System.out.println(this.connection.getInetAddress().toString() + " Disconnected");
    	synchronized(this){
    		if(in != null){
    			in.close();
    		}
    		if(out != null){
    			out.close();
    		}
    		if(!connection.isClosed()){
	    		connection.shutdownOutput();
	    		connection.shutdownInput();
	    		connection.close();
    		}
    	}
        
    }

    /**
     * Login handler
     *
     * @param input Login command
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void login(final DataPacket input) throws IOException {
        	
	    //login through the database
	    server.login(input);
	
	    //if there's a user, save it here locally
	    if (input.getPlayer() != null) {
	        setPlayer(input.getPlayer());
	        if(getPlayer().getGameHistory() == null){
	        	getPlayer().setGameHistory(new ArrayList<Game>());
	        }
	        if(getPlayer().getFriends() == null){
	        	getPlayer().setFriends(new ArrayList<Player>());
	        }
	        this.getPlayer().setStatus(PlayerStatus.ONLINE);
	        this.setName(this.getPlayer().getUsername());
	    }else{
	
	    	//login failed
	    	setPlayer(null);
	    }
        this.transferDataPacket(input);
	    System.out.println("ServerThread: LOGIN result: " + input.getCommand());
    }

    /**
     * Register handler
     *
     * @param input {@code DataPacket} that holds register information
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void register(final DataPacket input) throws IOException {
    	input.setPlayer(DB.getDB().register(input.getPlayer()));
        if(input.getPlayer() == null){
        	input.setCommand(Commands.ALREADY_EXISTS);      	
        }else{
        	input.setCommand(Commands.REGISTER);
        	setPlayer(input.getPlayer());
        	getPlayer().setGameHistory(new ArrayList<Game>());
        	getPlayer().setFriends(new ArrayList<Player>());
        	this.player.setStatus(PlayerStatus.ONLINE);
        }
        this.transferDataPacket(input);
        System.out.println("ServerThread: REGISTER result: " + input.getCommand());
    }
    
    /**
     * Requests a top 10 list of users from the database and sends it back to the
     * client
     * @throws IOException 
     * @see DB#getTop10() The getTop10 method from the Database(Which is in use here)
     */
    public void getTop10() throws IOException{
    	DataPacket dp = new DataPacket();
        out.reset();
    	dp.setCommand(Commands.TOP10);
        dp.setPlayers(DB.getDB().getTop10Players());
        System.out.println("ServerThread: GET TOP 10: " + dp.getPlayers());
        this.transferDataPacket(dp);
        System.out.println("SENT OUT TOP 10");   
    }

    /**
     * Transfer a {@code DataPacket} from the server to the client
     *
     * @param object the object to be sent
     */
    public void transferDataPacket(final DataPacket dp){
        try {
        	
        	//Java uses object references when sending objects through the stream
            //in some cases(when sending the same type of object with the same reference),
        	//an old reference will be sent, therefore reset() method
            //resets the references and lets the modified object to be sent over
        	String json = dp.toJson();
        	
        	synchronized(this){
        		out.reset();
				out.writeObject(json);
				out.flush();
        	}
        	
	        System.out.println("ServerThread: DATA SENT: " + json);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("ServerThread: Failed to broadcast data");
		}
    }

    /**
     * User wished to disconnect
     *
     * @throws IOException
     */
    private void disconnect() throws IOException {
    	if(getPlayer() != null){
	        System.out.println(getPlayer().getUsername() + " DISCONNECT request");
	        if(game != null){
	        	getGame().removePlayer(this);
	        	setGame(null);
	        	final DataPacket disconnect = new DataPacket();
	        	disconnect.setCommand(Commands.DISCONNECT);
	        	disconnect.setPlayer(this.getPlayer());
	        	transferDataPacket(disconnect);
	        }else{ 
	        	
	        	//in case the user logged in or registered and went back to main menu (Socket is still open)
	        	//this was done because of problems with "Already connected" issues
	        	getPlayer().setStatus(PlayerStatus.OFFLINE);
	        	setPlayer(null);
	        }
    	}else{
    		
    		//User exits the program (Last disconnect)
    		try {
				finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    @Override
    protected void finalize() throws Throwable {
		server.removeClient(this);
        closeCrap();
        this.interrupt();
    	super.finalize();
    }
}
