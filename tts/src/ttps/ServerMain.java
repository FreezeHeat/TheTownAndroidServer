/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ttps;

/**
 *
 * The main server program, runs the server instance
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see Code.Server
 * @since 1.0
 */
public class ServerMain {

    /**
     * Starts the main program of the server side
     * @param args arguments to be sent (not in use here)
     */
    public static void main(final String[] args) {
        int port = 55555;
        int numClients = 50;  
        Server server = null;
        
        server = new Server();
        server.startServer(port, numClients);
    }
}
