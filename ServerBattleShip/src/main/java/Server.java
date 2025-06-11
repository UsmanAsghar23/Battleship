import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;


public class Server {
    int count = 1;
    ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
    TheServer server;
    private Consumer<Serializable> callback;
    ArrayList<String> ssUserNameList = new ArrayList<>();
    private Consumer<ArrayList<String>> guiUpdateCallback;


    // Add a new field to track player states
    HashMap<String, Boolean> playerReadyStatus = new HashMap<>();


    Server(Consumer<Serializable> call) {
        callback = call;
        server = new TheServer();
        server.start();
    }


    public class TheServer extends Thread {


        public void run() {
            try (ServerSocket mySocket = new ServerSocket(5555);) {
                System.out.println("Server is waiting for a client!");
                while (true) {
                    ClientThread c = new ClientThread(mySocket.accept(), count);
                    clients.add(c);
                    c.start();
                    count++;
                }
            }//end of try
            catch (Exception e) {
                callback.accept("Server socket did not launch");
            }
        }//end of while
    }
    class ClientThread extends Thread {
        Socket connection;
        int count;
        ObjectInputStream in;
        ObjectOutputStream out;
        String userName;


        ClientThread(Socket s, int count) {
            this.connection = s;
            this.count = count;
        }


        public String getUsername() {
            return userName;
        }


        public void updateClients(Message message) {
            for (ClientThread c : clients) {
                try {
                    c.send(message);
                } catch (Exception ignored) {
                }
            }
        }


        public void updateSpecificClient(String name, Message message) {
            for (ClientThread c : clients) {
                if (c.userName.equals(name)) {
                    try {
                        c.send(message);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                connection.setTcpNoDelay(true);
            } catch (Exception e) {
                System.out.println("Streams not open");
            }


            while (true) {
                try {
                    Message data = (Message) in.readObject(); //receiving from the client
                    callback.accept(data);
                    switch (data.type) {
                        case LOGIN_MESSAGE: {
                            userName = data.userName;
                            if (!ssUserNameList.contains(userName)) {
                                ssUserNameList.add(userName);
                                Message responseMessage = new Message(Message.Type.LOGIN_RESPONSE, userName, false, null, null, null, null);
                                send(responseMessage);
                                Message updateMessage = new Message(Message.Type.UPDATE_USERS, null, false, ssUserNameList, null, null, null);
                                updateClients(updateMessage);
                            } else {
                                Message responseMessage = new Message(Message.Type.LOGIN_RESPONSE, null, true, null, null, null, null);
                                send(responseMessage);
                            }
                            break;
                        }
                        case DIRECT_MESSAGE: {
                            updateSpecificClient(data.recipientID, new Message(Message.Type.DIRECT_MESSAGE, null, false, null, data.recipientID, data.senderID, data.messageContent));
                            break;
                        }
                        case GLOBAL_MESSAGE: {
                            updateClients(new Message(Message.Type.GLOBAL_MESSAGE, null, false, null, null, data.senderID, data.messageContent));
                            break;
                        }
                        case UPDATE_USERS: {
                            break;
                        }
                        case LOGIN_RESPONSE:


                            //Handle attack case
                        case ATTACK: {
                            //SEND YOUR ATTACK TO THE OPPENENT
                            updateSpecificClient(data.recipientID, new Message(Message.Type.ATTACK, data.senderID, data.recipientID, data.x, data.y, data.gameStatus));
                            break;
                        }
                        //Handle placeship


                        case PLACE_SHIP: {
                            //SEND YOUR SHIP PLACEMENT TO THE OPPONENT BOARD
                            updateSpecificClient(data.recipientID, new Message(Message.Type.PLACE_SHIP, data.senderID, data.recipientID, data.x, data.y, data.gameStatus));
                            //What does gamestatus mean for us?
                            //Sometimes it means running, or won or lost
                            //gamestatus will be a placeholder for Vertile or horzontal
                            break;
                        }
                        //handle game status update
                        case GAME_STATE_UPDATE: {
                            if (data.gameStatus.equals("READY")) {
                                playerReadyStatus.put(userName, true); // Mark this player as ready
                                callback.accept(userName + " is ready");
                                // Check if all connected players are ready
                                if (playerReadyStatus.size() == clients.size() && playerReadyStatus.values().stream().allMatch(status -> status)) {
                                    // If all players are ready, notify all clients to proceed
                                    Message gameStateUpdate = new Message(Message.Type.GAME_STATE_UPDATE, null, null, data.x, data.y, "BOTH_READY");
                                    updateClients(gameStateUpdate); // Send to all clients to transition scenes

                                    // Optionally, reset the ready status if needed for a new game round
                                    playerReadyStatus.clear();
                                }
                            }

                            break;
                        }


                        //SEND PEOPLES TURNS
                        // Handle player turns
                        case TURNS: {
                            if (data.gameStatus.equals("true")) {
                                // Retrieve the current player's index based on their username in the client list
                                int currentPlayerIndex = -1;
                                for (int i = 0; i < clients.size(); i++) {
                                    if (clients.get(i).getUsername().equals(userName)) {
                                        currentPlayerIndex = i;
                                        break;
                                    }
                                }
                                // Calculate the next player's index
                                int nextPlayerIndex = (currentPlayerIndex + 1) % clients.size(); // This ensures rotation in a circular manner
                                // Get the next player
                                ClientThread nextPlayer = clients.get(nextPlayerIndex);
                                // Send turn message to the next player
                                Message turnMessage = new Message(Message.Type.TURNS, data.recipientID, data.senderID, data.x, data.y, "true");
                                updateSpecificClient(nextPlayer.getUsername(), turnMessage);

                                // Notify other players to wait
                                for (ClientThread client : clients) {
                                    if (client != nextPlayer) {
                                        // Construct the wait message with the correct username
                                        Message waitMessage = new Message(Message.Type.TURNS, data.recipientID, data.senderID,  data.x, data.y, "false");
                                        updateSpecificClient(client.getUsername(), waitMessage);
                                        System.out.println("Notifying " + client.getUsername() + " to wait for their turn");
                                    }
                                }

                            }
                            break;
                        }
                        //SEND IF A GAME iS LOST
                        case END_GAME: {
                            if(Objects.equals(data.gameStatus, "CONGRATS")){
                                updateSpecificClient(data.recipientID,  new Message(Message.Type.END_GAME, data.senderID, data.recipientID, data.x, data.y, data.gameStatus));
                            }
                            //TODO
                            break;
                        }
                        case BUTTONS:{
                            if(data.attackHitOrMiss != null){
                                System.out.println("The server has a attack from" + data.senderID  +"that is a " + data.attackHitOrMiss + data.recipientID + "AT "+ data.x + " "+data.y );
                                if(data.attackHitOrMiss){
                                    updateSpecificClient(data.recipientID, new Message(Message.Type.BUTTONS, data.senderID, data.recipientID, data.attackHitOrMiss, data.gameStatus, data.x, data.y));
                                }
                                else{
                                    updateSpecificClient(data.recipientID, new Message(Message.Type.BUTTONS, data.senderID, data.recipientID, data.attackHitOrMiss, data.gameStatus, data.x, data.y));
                                }
                            }
                            break;
                        }
                        default:
                            break;
                    }
                } catch (Exception e) {
                    clients.remove(this);
                    ssUserNameList.remove(this.userName);
                    updateClients(new Message(Message.Type.UPDATE_USERS, null, false, ssUserNameList, null, null, null));
                    break;
                }
            }
        }//end of run


        public void send(Message data) {
            try {
                out.reset();
                out.writeObject(data);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }//end of client thread
}


