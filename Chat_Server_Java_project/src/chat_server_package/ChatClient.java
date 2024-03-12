package chat_server_package;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    
    DefaultListModel<String> listModel = new DefaultListModel<String>();
    JList<String> listBox = new JList<String>(listModel);
    
    //Adding checkbox to enable broadcast feature
    JCheckBox broadcastTick = new JCheckBox("broadcast");
    
    String clientName;
    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {
    	
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "South");
        
        //Adding listBox GUI pane to center to display client list 
        frame.getContentPane().add(new JScrollPane(listBox), "Center");
        //Adding Checkbox GUI pane to West to display the checkbox.
        frame.getContentPane().add(broadcastTick,"West");
        frame.pack();
        
        //Making the broadcast feature is the default method of messaging
        broadcastTick.setSelected(true);
        
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server. Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
            	
            	//Check for the messaging method
            	if(broadcastTick.isSelected()) {
            		//If boradcast is selected the default way of handling messages will run
            		out.println(textField.getText());
                    textField.setText("");
            	}else {
            		//Getting indexes of selected items in listBox and assigning them in a Integer array
            		int selectedClients[] = listBox.getSelectedIndices();
            		
            		//Looping over each selected client to send them the messages in p2p manner
            		for(int k = 0; k < selectedClients.length; k++) {
            			//Gets the client name associated with the given index
            			String choosenClients = listModel.get(selectedClients[k]).toString();
            			//Writing messages into output stream in (Name>>message)format
            			out.println(choosenClients + ">>" + textField.getText());
            		}
            	}
                
            }
        });
        
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
    	//Storing client name in a String - myName
        String myName = JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
        //Assigning myName to clientName to make client name available to other functions
        clientName = myName;
        return myName;
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
       
        // Process all messages from server, according to the protocol.
        
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
                
            }
            //Handling connected client names received from the server
            else if(line.startsWith("CLIENTLIST")) {
            	//Getting connected client names from the line. substring is started from 10 since line starts with "CLIENTSLIST" string which has a length of 10. 
            	String clientListString = line.substring(10);
            	//Separating client names by "," and storing names in a String array
            	String clientNamesArray[] = clientListString.split(",");
            	//Clearing listModel to accommodate new client list
            	listModel.clear();
            	
            	int index = 0;
            	for(int j = 0; j < clientNamesArray.length;j++) {
            		//Skipping name assignment to the list if client name is equal to the client name
            		if(clientNamesArray[j].equals(clientName)) {
            			continue;
            		}
            		//Assigning client names to the listModel
            		listModel.add(index, clientNamesArray[j]);
            	}
            }
        }
    }

    /**
     * Runs the client as an application with a closable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}