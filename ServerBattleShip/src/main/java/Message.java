/**
 Program 4: BS
 Course: CS 342, Spring 2024, UIC
 Authors: Mohammad Ayesh and Shareek Shaffie and Yusuf Ahmedjeelani and Usman Asghar
 NetID: mayes3 and smoha45 and yahme7 and uasgh2
 File Description: This Java file implements messaging app
 */

import java.io.Serializable;
import java.util.ArrayList;



public class Message implements Serializable {
    static final long serialVersionUID = 42L;


    public enum Type {
        INVALID, LOGIN_MESSAGE, LOGIN_RESPONSE, DIRECT_MESSAGE, GLOBAL_MESSAGE, UPDATE_USERS, PLACE_SHIP, ATTACK, GAME_STATE_UPDATE, TURNS, END_GAME, BUTTONS, ATTACK_NOTIFICATION
    }


    public int x,y;
    public String gameStatus;
    public Type type;
    public String senderID;
    public String recipientID;
    //userID
    public String messageContent;
    public String userName;
    public String direction;
    public Boolean attackHitOrMiss = null;
    public boolean isDuplicate = false;
    ArrayList<String> usernameList;


    Message(Type type, String userName, boolean isDuplicate, ArrayList<String> usernameList, String recipientID, String senderID, String messageContent){
        this.type = type;
        this.userName = userName;
        this.isDuplicate = isDuplicate;
        this.usernameList = usernameList;
        this.recipientID = recipientID;
        this.senderID = senderID;
        this.messageContent = messageContent;
    }


    // constructor for game action
    Message(Type type, String senderID, String recipientID,  int x, int y, String gameStatus) {
        this.type = type;
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.x = x;
        this.y = y;
        this.gameStatus = gameStatus;
    }
    Message(Type type, String senderID, String recipientID, Boolean result, String gameStatus, int x, int y) {
        this.type = type;
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.attackHitOrMiss = result;
        this.gameStatus = gameStatus;
        this.x = x;
        this.y = y;
    }


    @Override
    public String toString() {
        switch (type) {
            case GLOBAL_MESSAGE: {
                return "Global Message from: \"" + senderID + "\" to: everyone with contents \"" + messageContent + "\"";
            }
            case DIRECT_MESSAGE: {
                return "Direct Message from: \"" + senderID + "\" to: \"" + recipientID + "\" with contents \"" + messageContent + "\"";
            }
            case LOGIN_MESSAGE: {
                return "Login Attempt with name \"" + userName + "\"";
            }
            case LOGIN_RESPONSE: {
                return "Login Response with duplicate check \"" + isDuplicate + "\"";
            }
            case UPDATE_USERS: {
                return "Updating Users with: \"" + usernameList + "\"";
            }
            case PLACE_SHIP: {
                return "Place Ship by: \"" + senderID + "\" at coordinates (" + x + ", " + y + ")";
            }
            case ATTACK: {
                return "Attack by: \"" + senderID + "\" at coordinates (" + x + ", " + y + ")";
            }
            case GAME_STATE_UPDATE: {
                return "Game State Update by: \"" + senderID + "\" status: \"" + gameStatus + "\"";
            }
            case TURNS: {
                return "Turn made by: \"" + senderID + "\"";
            }
            case END_GAME: {
                return "ATTENTION: \"" + senderID + "\" status: \"" + gameStatus + "\"";
            }
            case BUTTONS: {
                return "Button Pressed";
            }
            case ATTACK_NOTIFICATION: {
                return gameStatus;
            }
            case INVALID:
            default: {
                return "Invalid Message";
            }
        }
    }
}


