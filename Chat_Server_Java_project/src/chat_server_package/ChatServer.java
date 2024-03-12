package chat_server_package;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    //Using a HashMap to store connected Clients names
    private static HashMap<String,PrintWriter> connectedClientMap = new HashMap<String,PrintWriter>();
    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
            	Socket socket  = listener.accept();
                Thread handlerThread = new Thread(new Handler(socket));
                handlerThread.start();
            }
        } finally {
            listener.close();
        }
    }
    
   
    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
            
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    
                    // Adding synchronized block to ensure the thread safety of the
                    // the shared variable 'names'
                    synchronized(names) {
                    if (!names.contains(name)) {
                            names.add(name);
                            connectedClientMap.put(name, out);
                            break;
                        }
                    }
                 }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);
                
                updateClientsList();
                
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    
                    
                    //Checks whether message is intend for a specific client(Unicast)
                    if(isUnicastMsg(input)) {
                    	//Getting the Messages associated with the client name (Sender). Then send it to the output stream. In here I replaced unicast message format to avoid message duplication
                    	connectedClientMap.get(name).println("MESSAGE " + name + ": " + input.replace(getUnicastClient(input)+">>",""));
                    	//Getting the Messages associated with the recipient name (Recipient). Then send it to the output stream. 
                    	connectedClientMap.get(getUnicastClient(input)).println("MESSAGE " + name + ": " + input.replace(getUnicastClient(input) + ">>", ""));
                    }else {
                    	//This is default broadcast method. Runs if message type is not a unicast (or multicast)
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                    }
                    
                    
                }
            }
            //In here I'm handling the socket exception separately from the other IO exceptions
            catch (SocketException se) {
                // Handling SocketException separately
                System.out.println("SocketException occurred: " + se.getMessage());
            } 
            catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
            	
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
                //Removing name from ConnecteClientMap HashMap
                connectedClientMap.remove(name);
            }
        }
        
        //Check if the message is intended for a specific client
        public boolean isUnicastMsg(String message) {
            for (String clientName : names) {
                String clientPrefix = clientName + ">>";
                if (message.contains(clientPrefix)) {
                    return true;
                }
            }
            return false;
        }

        //Getting the recipient name from a unicast message
        public String getUnicastClient(String message) {
            String unicastClient = null;
            for (String clientName : names) {
                String clientPrefix = clientName + ">>";
                if (message.contains(clientPrefix)) {
                    unicastClient = clientName;
                    break;
                }
            }
            return unicastClient;
        }
        
      //Returns a list of connected clients 
        public static String getAllClients() {
        	
        	//Getting the connected client names from connectedClientMap and assigning them into a String array
            String[] keys = connectedClientMap.keySet().toArray(new String[0]);
            String clientArray = "";
            for (int i = 0; i < keys.length; i++) {
            	clientArray = clientArray + "," + keys[i];
            	
            }
            
			return clientArray;
        }

        //Sends the current client list when a new client is added
        public void updateClientsList() {
            for (PrintWriter writer : writers) {
            	//Writing the latest connected client list to the output stream
                writer.println("CLIENTLIST" + getAllClients());
                
            }
        }

    }
}