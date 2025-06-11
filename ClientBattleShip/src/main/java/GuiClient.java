import java.util.*;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import static java.sql.Types.NULL;

public class GuiClient extends Application {
    private Stage mainStage; // Class-level variable to store the primary stage
    TextField usernameInput;
    TextField chatBox;
    Button sendButton;
    Button submitButton;
    Button onlineButton;
    Button aiButton;
    Button exitButton = new Button("Exit");
    HashMap<String, Scene> sceneMap;
    Client clientConnection;
    Button backButton = new Button("Back");
    Map<String, List<String>> groupChatList = new HashMap<>();
    ListView<String> messageDisplayArea = new ListView<>();
    ListView<String> directMessageDisplayArea = new ListView<>();
    Label directMessageLabel;
    String titleUserName;
    String selectedUser = null;
    ArrayList<String> ccUserNames = new ArrayList<String>();
    ListView<String> userNameList = new ListView<>();
    ArrayList<String> globalChat = new ArrayList<>();
    HashMap<String, ArrayList<String>> dmChats = new HashMap<String, ArrayList<String>>();
    boolean isAIgame = false;
    //List<int[]> hitList = new ArrayList<>();
    Random rand = new Random();
    boolean targetingMode = false;
    Queue<int[]> attackQueue = new LinkedList<>();

    board localBoard = new board();
    board oppenentBoard = new board();
    //GRIDS
    GridPane opponentGrid; //LOOK HERE
    GridPane myGrid;
    private HashMap<Integer, Boolean> shipAvailability;
    private boolean isHorizontal = true;
    private VBox shipPanel; // Declaration at the class level
    private HashMap<Integer, List<Button>> placedShips = new HashMap<>();
    private int HitCounter = 0; //COUNTING FOR LOSSES
    private Button confrimButton;
    private Button resetBoardButton;
    private Button[][] gridButtons = new Button[10][10];  // Assuming a 10x10 grid
    private boolean isHit;
    boolean player1Confirmed = false;
    boolean player2Confirmed = false;
    private int aiHitCounter = 0;

    private ArrayList<String> getDM(String otherUser) {
        if (!dmChats.containsKey(otherUser)) {
            dmChats.put(otherUser, new ArrayList<String>());
        }
        return dmChats.get(otherUser);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.mainStage = primaryStage; // Initialize class variable with the provided primaryStage
        shipAvailability = new HashMap<>();
        shipAvailability.put(5, true);
        shipAvailability.put(4, true);
        shipAvailability.put(3, true); // First size 3 ship
        shipAvailability.put(3 + 100, true); // Second size 3 ship, using a different key
        shipAvailability.put(2, true);
        confrimButton = new Button("Confirm Board");

        confrimButton.setDisable(true);
        clientConnection = new Client(data -> {
            Platform.runLater(() -> {
                Message msg = (Message) data;
                System.out.println(msg);
                switch (msg.type) {
                    case LOGIN_RESPONSE: {
                        if (!msg.isDuplicate) {
                            titleUserName = msg.userName;
                            primaryStage.setScene(sceneMap.get("gameMode"));
                            primaryStage.setTitle("Connected as: " + titleUserName);
                        } else {
                            usernameInput.setPromptText("Duplicate Name");
                            usernameInput.clear();
                        }
                        break;
                    }
                    case UPDATE_USERS: {
                        ccUserNames.clear();
                        ccUserNames.addAll(msg.usernameList);
                        refreshUsernamesListView();
                        break;
                    }
                    case GLOBAL_MESSAGE: {
                        globalChat.add(msg.senderID + ": " + msg.messageContent);
                        refreshGlobalMessagesListView();
                        break;
                    }
                    case DIRECT_MESSAGE: {
                        getDM(msg.senderID).add(msg.senderID + ": " + msg.messageContent);
                        if (selectedUser.equals(msg.senderID)) {
                            refreshDirectMessagesListView();
                        }
                        break;
                    }
                    case ATTACK: {
                        // Server determines if the attack is a hit or miss
                        boolean isHit = localBoard.attack(msg.x, msg.y);

                        // Increment hit counter and check for game over condition
                        // moe has stuff in this
                        if(isHit) {
                            HitCounter++;
                        }

                        // Send a response back to the server about the hit or miss status
                        Message response = new Message(Message.Type.BUTTONS, msg.recipientID, msg.senderID, isHit, null, msg.x, msg.y);
                        clientConnection.send(response);

                        // Update GUI on the JavaFX main thread
                        Platform.runLater(() -> {
                            updateButtonOnMyGrid(msg.y, msg.x, isHit);
                        });
                        break;
                    }
                    case PLACE_SHIP: {
                        //CODE NEEDS TO BE PLACED HERE TO HANDLE PUTTING opponent SHIPS ON attack board maybe not actually
                        break;
                    }
                    case GAME_STATE_UPDATE: {
                        if(Objects.equals(msg.gameStatus, "BOTH_READY")) {
                            Platform.runLater(() -> {
                                primaryStage.setScene(sceneMap.get("directMessage"));  // Assuming "game" is the key for your game scene
                            });
                        }
                        //HANDLES IF YOU WIN
                        if(Objects.equals(msg.gameStatus, "CONGRATS")){
                            primaryStage.setScene(createWinScene());
                        }
                        break;
                    }
                    //TELLS YOU IT IS YOUR TURN
                    // Then, modify the TURNS case in your switch statement:
                    case TURNS: {
                        if (Objects.equals(msg.gameStatus, "true")) {
                            Platform.runLater(() -> {
                                primaryStage.setScene(sceneMap.get("directMessage"));
                                if (isAIgame) {
                                    if (HitCounter >= 17) {  // Assuming 17 is the total number of hits required to win
                                        primaryStage.setScene(createWinScene());
                                        // Send a message to the opponent declaring a win
                                        Message winMsg = new Message(Message.Type.END_GAME, titleUserName, selectedUser, msg.x, msg.y, "CONGRATS");
                                        clientConnection.send(winMsg);
                                    }
                                }
                                else {
                                    if (HitCounter >= 17) {  // Assuming 17 is the total number of hits required to win
                                        primaryStage.setScene(createLoseScene());
                                        // Send a message to the opponent declaring a win
                                        Message winMsg = new Message(Message.Type.END_GAME, titleUserName, selectedUser, msg.x, msg.y, "CONGRATS");
                                        clientConnection.send(winMsg);
                                    }
                                }
                            });
                        }
                        else if (Objects.equals(msg.gameStatus, "false")) {
                            Platform.runLater(() -> {
                                primaryStage.setScene(sceneMap.get("waitingForTurn"));
                            });
                        }
                        else if(msg.attackHitOrMiss != null){
                            if(((Message) data).attackHitOrMiss){
                                System.out.println("You hit them.");
                            }
                            else{
                                System.out.println("You missed them.");
                            }
                        }
                        else {
                            System.out.println("Unexpected game status received");
                        }
                        break;
                    }
                    //TELLS YOU IF GAME IS OVER
                    case END_GAME: {
                        if(Objects.equals(msg.gameStatus, "CONGRATS")){
                            primaryStage.setScene(createWinScene());
                            break;
                        }
                    }
                    case BUTTONS:{
                        if(((Message) data).attackHitOrMiss){
                            updateButtonOnOPPGrid(((Message) data).y, ((Message) data).x, ((Message) data).attackHitOrMiss);
                        }
                        else{
                            updateButtonOnOPPGrid(((Message) data).y, ((Message) data).x, ((Message) data).attackHitOrMiss);
                        }
                    }
                    default: {
                        break;
                    }
                }
            });
        });
        clientConnection.start();
        usernameInput = new TextField();
        usernameInput.setMaxWidth(200);

        submitButton = new Button("Submit");
        submitButton.setStyle("-fx-background-color: #848a0a; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-min-width: 120px; " +
                "-fx-min-height: 40px;" +
                "-fx-background-radius: 15;"
        );


        submitButton.setOnAction(e -> {
            String userName = usernameInput.getText().trim();
            if(!userName.isEmpty()){
                clientConnection.send(new Message(Message.Type.LOGIN_MESSAGE, userName, false, null, null, null, null));
            }
            usernameInput.clear();
        });

        chatBox = new TextField();
        sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            chatBox.getText();
        });

        sceneMap = new HashMap<String, Scene>();
        sceneMap.put("login", clientWelcomeGui());
        sceneMap.put("main", frontGui(primaryStage));
        sceneMap.put("gameMode", gameModeScene(primaryStage));
        sceneMap.put("directMessage", directMessageScene(primaryStage));
        sceneMap.put("waitScene", createWaitingScene());
        sceneMap.put("waitingForTurn", createWaitingForTurnScene());

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

        primaryStage.setScene(sceneMap.get("login"));
        primaryStage.show();
    }

    private void refreshUsernamesListView() {
        userNameList.getItems().setAll(ccUserNames);
        userNameList.refresh();
    }

    private void refreshGlobalMessagesListView() {
        messageDisplayArea.getItems().setAll(globalChat);
        messageDisplayArea.refresh();
    }

    private void refreshDirectMessagesListView() {
        directMessageDisplayArea.getItems().setAll(getDM(selectedUser));
        directMessageDisplayArea.refresh();
    }

    private void refreshDirectMessage() {
        refreshDirectMessagesListView();
        directMessageLabel.setText(selectedUser);
    }

    public Scene clientWelcomeGui() {
        // Load the image
        Image image = new Image("img2.jpeg", 750, 500, false, false);

        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        // Create your GUI components
        Label welcomeLabel = new Label("Battleship");
        welcomeLabel.setStyle("-fx-font-size: 60px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");

        Label promptLabel = new Label("Enter Username:");
        promptLabel.setStyle("-fx-font-size: 20px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");

        VBox centerBox = new VBox(promptLabel, usernameInput, submitButton);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setMargin(promptLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(usernameInput, new Insets(0, 0, 0, 0));
        VBox.setMargin(submitButton, new Insets(10, 0, 0, 0));

        VBox topBox = new VBox(welcomeLabel);
        topBox.setAlignment(Pos.CENTER);

        // Create the BorderPane
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topBox);
        borderPane.setCenter(centerBox);
        borderPane.setBackground(backgroundObj); // Set the background

        BorderPane.setMargin(topBox, new Insets(200, 0, 0, 0));
        // Create and return the scene
        return new Scene(borderPane, 750, 500);
    }


    public Scene gameModeScene(Stage primaryStage){
        Image image2 = new Image("img3.jpeg", 750, 500, false, false);

        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        Label playBattleshipLabel = new Label("Select Game Mode");
        playBattleshipLabel.setStyle("-fx-font-size: 45px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");
        playBattleshipLabel.setAlignment(Pos.CENTER);

        onlineButton = new Button("Play Online");
        onlineButton.setStyle(
                "-fx-background-color: linear-gradient(#32cd92, #006400);" +
                        "-fx-background-radius: 30;" +
                        "-fx-background-inset: 0;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.6), 5, 0.0, 0, 1);"
        );        onlineButton.setOnMouseClicked(e -> {
            isAIgame = false;
            backButton.setDisable(isAIgame);
            primaryStage.setScene(sceneMap.get("main"));
        });

        aiButton = new Button("Play AI");
        aiButton.setStyle(
                "-fx-background-color: linear-gradient(#4d0cf3, #4d0cf3);" +
                        "-fx-background-radius: 30;" +
                        "-fx-background-inset: 0;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.6), 5, 0.0, 0, 1);"
        );
        aiButton.setOnMouseClicked(e -> {
            startAI();
            isAIgame = true;
            backButton.setDisable(isAIgame);
            selectedUser = "AI";
            if (!ccUserNames.contains("AI")) {
                ccUserNames.add("AI");
                refreshUsernamesListView();
            }
            primaryStage.setScene(sceneMap.get("directMessage"));
        });

        Button exitButton = new Button("Exit");
        exitButton.setStyle(
                "-fx-background-color: linear-gradient(#1b0008, #be1d60);" +
                        "-fx-background-radius: 30;" +
                        "-fx-background-inset: 0;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.6), 5, 0.0, 0, 1);" // Changed shadow color to white
        );
        exitButton.setOnAction(event -> {
            primaryStage.close();  // Closes the application window
        });

        HBox buttonsBox = new HBox(20, onlineButton, aiButton);
        buttonsBox.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(20, playBattleshipLabel, buttonsBox, exitButton);
        centerBox.setAlignment(Pos.CENTER);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(centerBox);
        borderPane.setBackground(backgroundObj);

        return new Scene(borderPane, 750, 500);
    }

    public Scene frontGui(Stage primaryStage) {
        Image image2 = new Image("img4.jpeg", 750, 500, false, false);

        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        VBox messageBox = new VBox(messageDisplayArea);
        messageBox.setAlignment(Pos.CENTER);
        VBox.setMargin(messageDisplayArea, new Insets(0, 5, 0, 5));

        Label selectToPlayLabel = new Label("Select to Play");
        selectToPlayLabel.setStyle("-fx-font-size: 14px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");

        userNameList = new ListView<>();
        userNameList.getItems().addAll(ccUserNames);

        VBox userSidebar = new VBox();
        userSidebar.setPrefWidth(100);
        userSidebar.setAlignment(Pos.CENTER);
        userSidebar.getChildren().addAll(selectToPlayLabel, userNameList);
        userSidebar.setSpacing(10);
        VBox.setMargin(userNameList, new Insets(0, 10, 0, 5));

        userNameList.setOnMouseClicked(e -> {
            selectedUser = userNameList.getSelectionModel().getSelectedItem();
            if (!selectedUser.equals(titleUserName)) {
                if (selectedUser != null) {
                    refreshDirectMessage();
                    primaryStage.setScene(sceneMap.get("directMessage"));
                }
            } else {
                selectedUser = null;
            }
        });

        //message input and send
        TextField messageInputField = new TextField();
        messageInputField.setPromptText("Enter message");

        Button sendMessageButton = new Button("Send");
        sendMessageButton.setOnAction(e -> {
            String messageText = messageInputField.getText();
            if (!messageText.isEmpty()) {
                // Send global message
                clientConnection.send(new Message(Message.Type.GLOBAL_MESSAGE, null, false, null, null, titleUserName, messageText));
                messageInputField.clear();
            }
        });

        HBox messageInputArea = new HBox(messageInputField, sendMessageButton);
        messageInputArea.setAlignment(Pos.CENTER);
        messageInputArea.setPadding(new Insets(10));
        HBox.setHgrow(messageInputField, Priority.ALWAYS);

        Label mainChatLabel = new Label("Game Chat");
        mainChatLabel.setStyle("-fx-font-size: 20px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");
        HBox serverNameHeader = new HBox(mainChatLabel);
        serverNameHeader.setAlignment(Pos.CENTER);
        serverNameHeader.setPadding(new Insets(10,0,0,0));

        // Main layout composition
        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(messageBox);
        mainLayout.setBottom(messageInputArea);
        mainLayout.setTop(serverNameHeader);
        mainLayout.setRight(userSidebar);
        mainLayout.setBackground(backgroundObj);

        return new Scene(mainLayout, 750, 500);
    }


    private Scene createWaitingForTurnScene() {
        Label waitingLabel = new Label("Waiting for opponent to make their move...");
        // Set style with font and text color using CSS properties
        waitingLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 24; -fx-text-fill: white;");

        StackPane layout = new StackPane(waitingLabel);
        layout.setStyle("-fx-background-color: black;");
        layout.setAlignment(Pos.CENTER);

        return new Scene(layout, 800, 700);
    }

    private GridPane createAttackGrid() {
        GridPane battleshipBoard = new GridPane();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button button = new Button();
                button.setPrefSize(30, 30);
                button.setDisable(true);
                // Add functionality to buttons here (e.g., setting ships, handling attacks)
                battleshipBoard.add(button, j, i);
                int finalI = i;
                int finalJ = j;
                button.setOnAction(e -> {
                    // Define action when grid buttons are clicked
                    button.setDisable(true);
                    //JAVAFX GOES COL THEN ROW
                    Message outMessage = new Message(Message.Type.ATTACK, titleUserName, selectedUser, finalI, finalJ, "Active");
                    clientConnection.send(outMessage);
                    processPlayerAttack(finalI,finalJ);

                    // Switch to the waiting screen immediately after making an attack
                    Platform.runLater(() -> {
                        Message newMessage = new Message(Message.Type.TURNS, titleUserName, selectedUser, finalI, finalJ, "true");
                        clientConnection.send(newMessage);
                        mainStage.setScene(sceneMap.get("waitingForTurn"));
                    });
                });
            }
        }
        return battleshipBoard;
    }

    // Method to update the button appearance after an attack
    // Method that sends the attack and updates the button on your grid
    // Method to update your attack grid based on the attack result
    private void updateButtonOnMyGrid(int x, int y, boolean isHit) {
        Button button = getButtonFromGrid(opponentGrid, x, y);  // Retrieves button from your attack grid
        if (isHit) {
            button.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        } else {
            button.setStyle("-fx-background-color: blue; -fx-text-fill: white;");
        }

    }

    private void updateButtonOnOPPGrid(int x, int y, boolean isHit) {
        Button button1 = getButtonFromGrid(myGrid, x, y);  // Retrieves button from your attack grid
        if (isHit) {
            button1.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        } else {
            button1.setStyle("-fx-background-color: blue; -fx-text-fill: white;");
        }
    }

    // Method to get a button from a grid based on coordinates
    private Button getButtonFromGrid(GridPane grid, int x, int y) {
        for (Node node : grid.getChildren()) {
            if (GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null) {
                if (GridPane.getColumnIndex(node) == x && GridPane.getRowIndex(node) == y) {
                    return (Button) node;
                }
            }
        }
        return null;  // Return null if no button is found (shouldn't happen)
    }

    private GridPane createGameGrid() {
        GridPane gridPane = new GridPane();
        //COLS
        for (int i = 0; i < 10; i++) {
            //ROWS
            for (int j = 0; j < 10; j++) {
                final int finalI = i;
                final int finalJ = j;
                Button cell = new Button();
                cell.setPrefSize(30, 30);
                gridButtons[i][j] = cell;  // Store the button in the array

                // Allow dropping onto the cell
                cell.setOnDragOver(event -> {
                    if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });
                cell.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    boolean success = false;
                    if (db.hasString()) {
                        String[] parts = db.getString().split(",");
                        int shipSize = Integer.parseInt(parts[0]);
                        int shipKey = Integer.parseInt(parts[1]);
                        if (isHorizontal) {
                            placeShip(gridPane, finalJ, finalI, shipSize, shipKey);
                        } else {
                            placeShip(gridPane, finalJ, finalI, shipSize, shipKey);
                        }
                        success = true;

                    }
                    event.setDropCompleted(success);
                    event.consume();
                });
                gridPane.add(cell, j, i);
            }
        }
        gridPane.setGridLinesVisible(true);
        return gridPane;
    }

    //IN THIS INSTANCE X REPRSENTS COLS AND Y REPRESRNEST ROWS
    private void placeShip(GridPane grid, int x, int y, int shipSize, int shipKey) {
        List<Button> shipButtons = new ArrayList<>();
        boolean collisionDetected = false;
        boolean successPlacement = false;
        // Check for valid placement and update the local board
        //CHANGED HERE
        if (isHorizontal) {
            successPlacement = localBoard.placeShip(y, x, shipSize, true);
        } else {
            successPlacement = localBoard.placeShip(y, x, shipSize, false);
        }

        for (int i = 0; i < shipSize; i++) {
            int targetX = isHorizontal ? x + i : x;
            int targetY = isHorizontal ? y : y + i;
            if (targetX >= 10 || targetY >= 10) {
                collisionDetected = true;
                break;
            }
            Button btn = (Button) getNodeFromGridPane(grid, targetX, targetY);
            if (btn != null && btn.getStyle().contains("darkblue")) {
                collisionDetected = true;
                break;
            }
            shipButtons.add(btn);
        }
        if (collisionDetected) {
            System.out.println("Collision detected: Ship placement overlaps with another ship.");
            return;
        }
        for (Button btn : shipButtons) {
            btn.setStyle("-fx-background-color: darkblue; -fx-border-color: black;");
        }
        placedShips.put(shipKey, shipButtons);
        shipAvailability.put(shipKey, false);
        disableShipButton(shipKey);

        if (successPlacement) {
            placedShips.put(shipKey, shipButtons);
            shipAvailability.put(shipKey, false);
            disableShipButton(shipKey);
            checkAllShipsPlaced();  // Check if all ships are placed and update button state
        }
    }

    private void checkAllShipsPlaced() {
        boolean allPlaced = shipAvailability.values().stream().allMatch(placed -> !placed);
        confrimButton.setDisable(!allPlaced);  // Enable the button only if all ships are placed
    }

    private void resetBoard(GridPane grid) {
        // Clear placed ships and reset ship availability
        placedShips.clear(); // Ensure this hashmap is completely cleared
        shipAvailability.replaceAll((k, v) -> true); // Resetting ship availability to true for all keys
        checkAllShipsPlaced();
        //CHANGED THIS
        //LOOK HERE
        localBoard = new board();

        // Clear all buttons from the grid and re-add them
        grid.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button button = createEmptyButton(); // Create a new empty button
                button.setStyle("-fx-border-color: black;"); // Add black border style
                grid.add(button, j, i);
                // Attach drag-and-drop event handlers to the button
                attachDragAndDropHandlers(grid, button);
            }
        }
        // Enable all ship buttons in the ship panel
        if (shipPanel != null) {
            for (Node node : shipPanel.getChildren()) {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    button.setDisable(false);
                }
            }
        }
        confrimButton.setDisable(true);
    }

    private Button createEmptyButton() {
        Button button = new Button();
        button.setPrefSize(30, 30);
        return button;
    }

    // Method to attach drag-and-drop event handlers to a button
    private void attachDragAndDropHandlers(GridPane grid, Button button) {
        button.setOnDragOver(event -> {
            if (event.getGestureSource() != button && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        button.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String[] parts = db.getString().split(",");
                int shipSize = Integer.parseInt(parts[0]);
                int shipKey = Integer.parseInt(parts[1]);
                // Place the ship on the grid
                placeShip(grid, GridPane.getColumnIndex(button), GridPane.getRowIndex(button), shipSize, shipKey);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void enableShipButton(int shipKey) {
        for (Node node : shipPanel.getChildren()) {
            if (node instanceof Button && Integer.parseInt(node.getUserData().toString()) == shipKey) {
                node.setDisable(false);
            }
        }
    }

    private void disableShipButton(int shipKey) {
        for (Node node : shipPanel.getChildren()) {
            if (node instanceof Button && node.getUserData() != null) {
                Button button = (Button) node;
                if (Integer.parseInt(node.getUserData().toString()) == shipKey) {
                    button.setDisable(true);
                    break;
                }
            }
        }
    }

    private Button createShipButton(int shipSize, int shipKey) {
        Button ship = new Button(shipSize + "x");
        ship.setPrefSize(30 * shipSize, 30); // Assuming a horizontal ship that occupies `shipSize` cells
        ship.setStyle("-fx-background-color: navy; -fx-text-fill: white; -fx-border-color: white;");
        ship.setUserData(shipKey);
        ship.setDisable(!shipAvailability.getOrDefault(shipKey, true));
        // Make the ship draggable
        ship.setOnDragDetected(event -> {
            if(!ship.isDisabled()) {
                Dragboard db = ship.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(shipSize + "," + ship.getUserData()); // Pass the ship size as data
                db.setContent(content);
                event.consume();
            }
        });
        return ship;
    }

    private Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }

    private void setShipOrientation(boolean horizontal) {
        isHorizontal = horizontal;
    }

    private void startAI(){
        placeShipsAI(oppenentBoard);
        opponentGrid.setDisable(false);
    }

    private void placeShipsAI (board aiBoard) {
        Random rand = new Random();
        int[] shipSizes = {5, 4, 3, 3, 2}; // Ship sizes
        for (int size : shipSizes) {
            boolean placed = false;
            while (!placed) {
                int x = rand.nextInt(10);
                int y = rand.nextInt(10);
                boolean horizontal = rand.nextBoolean();
                placed = aiBoard.placeShip(y, x, size, horizontal);
            }
        }
    }

    private void processPlayerAttack(int x, int y) {
        if (isAIgame) {
            boolean hit = oppenentBoard.attack(x, y);
            updateButtonOnOPPGrid(y, x, hit);
            String result = hit ? "hit" : "miss";
            sendAttackToServer(x, y, titleUserName + " attacks AI at (" + x + ", " + y + ") and it's a " + result);
            if (hit) {
                HitCounter++;
                if (HitCounter >= 17) {
                    Platform.runLater(() -> mainStage.setScene(createWinScene()));
                }
            }
            Platform.runLater(this::aiTurn); // Schedule AI's turn
        }
    }


    private void playerTurn() {
        opponentGrid.setDisable(false); // Enable interactions for the player
        // Other logic to handle player's turn
    }


    private void aiTurn() {
        // Simulate a delay to mimic thinking time
        //printHitList();
        new Thread(() -> {
            try {
                Thread.sleep(2250); // Wait for 2.25 second before executing AI move
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                executeAITurn();
            });
        }).start();
    }


    private void executeAITurn() {
        int[] target;
        if (!attackQueue.isEmpty()) {
            target = attackQueue.poll(); // Dequeue the next target
        } else {
            target = getRandomTarget(); // Get a random target if the queue is empty
        }

        boolean hit = localBoard.attack(target[0], target[1]);
        updateButtonOnMyGrid(target[1], target[0], hit);  // Update your grid to show AI's attack result
        String result = hit ? "hit" : "miss";
        sendAttackToServer(target[0], target[1], "AI attacks " + titleUserName + " at (" + target[0] + ", " + target[1] + ") and it's a " + result);
        if (hit) {
            aiHitCounter++;
            registerHit(target[0],target[1] );
            if (aiHitCounter >= 17) {
                Platform.runLater(() -> mainStage.setScene(createLoseScene()));
            }
            else {
                Platform.runLater(this::playerTurn); // Hand turn back to the player
            }
        }
        else {
            Platform.runLater(this::playerTurn);
        }
    }


    private int[] getRandomTarget() {
        int x, y;
        do {
            x = rand.nextInt(10);
            y = rand.nextInt(10);
        } while (localBoard.getCell(x,y) == board.HIT || localBoard.getCell(x,y) == board.MISS); // Ensuring not to repeat targets
        return new int[]{x, y};
    }


    private boolean isValidTarget(int x, int y) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return false; // Check bounds
        if (localBoard.getCell(x, y) == board.HIT || localBoard.getCell(x, y) == board.MISS) return false; // Already attacked


        // Check if the target is already in the queue
        for (int[] arr : attackQueue) {
            if (arr[0] == x && arr[1] == y) return false;
        }


        return true;
    }

    private void registerHit(int x, int y) {
        targetingMode = true;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // down, up, right, left
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (isValidTarget(nx, ny)) {
                attackQueue.offer(new int[]{nx, ny});
            }
        }
    }

    // Reset when the target ship is sunk don't think need this anymore
    private void clearHits() {
        targetingMode = false;
        // hitList.clear();
    }

    public void sendAttackToServer(int x, int y, String attacker) {
        clientConnection.send(new Message(Message.Type.ATTACK_NOTIFICATION, titleUserName, null, x, y, attacker));
    }

    private VBox createShipPanel() {
        shipPanel = new VBox(10); // Initialization
        shipPanel.getChildren().add(createShipButton(5, 5));
        shipPanel.getChildren().add(createShipButton(4, 4));
        shipPanel.getChildren().add(createShipButton(3, 3)); // First size 3 ship
        shipPanel.getChildren().add(createShipButton(3, 103)); // Second size 3 ship
        shipPanel.getChildren().add(createShipButton(2, 2));
        Button placeHorizontalButton = new Button("Place Horizontal");
        Button placeVerticalButton = new Button("Place Vertical");

        String normalStyle =
                "-fx-background-color: linear-gradient(#d3d3d3, #808080);" + // Gradient from BlueViolet to a deep purple
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.6), 5, 0.0, 0, 1);";


// Hover style
        String hoverStyle =
                "-fx-background-color: linear-gradient(#e6e6e6, #a9a9a9);" + // Lighter purple gradient for hover
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.8), 5, 0.0, 0, 1);"; // Slightly stronger shadow for hover


// Applying the normal style initially
        placeHorizontalButton.setStyle(normalStyle);

// Change style on mouse hover
        placeHorizontalButton.setOnMouseEntered(e -> placeHorizontalButton.setStyle(hoverStyle));
        placeHorizontalButton.setOnMouseExited(e -> placeHorizontalButton.setStyle(normalStyle));


// Applying the normal style to the 'placeVerticalButton'
        placeVerticalButton.setStyle(normalStyle);

// Adding hover effects to the 'placeVerticalButton'
        placeVerticalButton.setOnMouseEntered(e -> placeVerticalButton.setStyle(hoverStyle));
        placeVerticalButton.setOnMouseExited(e -> placeVerticalButton.setStyle(normalStyle));

        // Reset board button
        resetBoardButton = new Button("Reset Board");
        resetBoardButton.setStyle(normalStyle);
        resetBoardButton.setOnMouseEntered(e -> resetBoardButton.setStyle(hoverStyle));
        resetBoardButton.setOnMouseExited(e -> resetBoardButton.setStyle(normalStyle));

        confrimButton.setStyle(normalStyle);
        confrimButton.setOnMouseEntered(e -> confrimButton.setStyle(hoverStyle));
        confrimButton.setOnMouseExited(e -> confrimButton.setStyle(normalStyle));

        resetBoardButton.setOnAction(e -> {
            resetBoard(opponentGrid);
            opponentGrid = createGameGrid();
            //LOOK HERE
        });  // Resets the opponentGrid

        confrimButton.setOnAction(e->{
            //FRONT END
            placeHorizontalButton.setDisable(true);
            placeVerticalButton.setDisable(true);
            resetBoardButton.setDisable(true);
            confrimButton.setDisable(true);

            for (Node node : myGrid.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(false);
                }
            }

            //BACK END
            Message outMessage = new Message(Message.Type.GAME_STATE_UPDATE, titleUserName, selectedUser, NULL, NULL, "READY");
            clientConnection.send(outMessage);
            mainStage.setScene(createWaitingScene());

            });
        placeHorizontalButton.setOnAction(e -> setShipOrientation(true));
        placeVerticalButton.setOnAction(e -> setShipOrientation(false));
        shipPanel.getChildren().addAll(placeHorizontalButton, placeVerticalButton, resetBoardButton, confrimButton);
        shipPanel.setAlignment(Pos.CENTER);
        shipPanel.setStyle("-fx-padding: 10; -fx-border-style: solid; -fx-border-width: 2; -fx-border-insets: 5; -fx-border-radius: 5;");
        return shipPanel;
    }

    private Scene createWaitingScene() {
        Label waitingLabel = new Label("Waiting For Other User to Confirm Board");
        waitingLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: white; -fx-font-weight: bold");

        StackPane layout = new StackPane(waitingLabel);
        layout.setStyle("-fx-background-color: black;");
        layout.setAlignment(Pos.CENTER);

        Scene waitingScene = new Scene(layout, 800, 700);
        return waitingScene;
    }

    private Scene createLoseScene() {
        Image image2 = new Image("lose.jpeg", 800, 700, false, false);

        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        StackPane layout = new StackPane();
        layout.setBackground(backgroundObj); // Apply the background image
        layout.setAlignment(Pos.CENTER);

        Scene loseScene = new Scene(layout, 800, 700);
        return loseScene;
    }


    private Scene createWinScene() {
        Image image2 = new Image("win.jpeg", 800, 700, false, false);
        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        StackPane layout = new StackPane();
        layout.setBackground(backgroundObj); // Apply the background image
        layout.setAlignment(Pos.CENTER);

        Scene winScene = new Scene(layout, 800, 700);
        return winScene;
    }


    private Scene directMessageScene(Stage primaryStage) {
        Image image2 = new Image("img4.jpeg", 800, 700, false, false);

        // Create a background image
        BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
        BackgroundImage background = new BackgroundImage(image2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        Background backgroundObj = new Background(background);

        // Create game grids and ship panel
        myGrid = createAttackGrid();
        opponentGrid = createGameGrid();
        VBox shipPanel = createShipPanel();

        Label opponentGridLabel = new Label("Opponent Grid");
        opponentGridLabel.setStyle("-fx-font-size: 16px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");


        Label myGridLabel = new Label("My Grid");
        myGridLabel.setStyle("-fx-font-size: 16px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");


        VBox opponentGridBox = new VBox(5, myGridLabel, opponentGrid);
        VBox myGridBox = new VBox(5, opponentGridLabel, myGrid);
        opponentGridBox.setAlignment(Pos.CENTER);
        myGridBox.setAlignment(Pos.CENTER);

        // VBox to hold both grids for better alignment and separation
        VBox gridsBox = new VBox(10, myGridBox, opponentGridBox);
        gridsBox.setAlignment(Pos.CENTER);


        // Message input area
        TextField messageInputField = new TextField();
        messageInputField.setPromptText("Enter message");
        Button sendMessageButton = new Button("Send");
        sendMessageButton.setOnAction(e -> {
            String messageContent = messageInputField.getText();
            if (!messageContent.isEmpty()) {
                clientConnection.send(new Message(Message.Type.DIRECT_MESSAGE, null, false, null, selectedUser, titleUserName, messageContent));
                getDM(selectedUser).add(titleUserName + ": " + messageContent);
                refreshDirectMessagesListView();
                messageInputField.clear();
            }
        });

        HBox messageInputArea = new HBox(10, messageInputField, sendMessageButton);
        messageInputArea.setAlignment(Pos.CENTER);
        messageInputArea.setPadding(new Insets(5));

        // Message display area
        VBox messageBox = new VBox(10, directMessageDisplayArea, messageInputArea);
        messageBox.setAlignment(Pos.CENTER);
        VBox.setMargin(directMessageDisplayArea, new Insets(0, 5, 5, 5));
        directMessageDisplayArea.setPrefWidth(200); // Adjust the preferred width as needed

        backButton.setStyle(
                "-fx-background-color: linear-gradient(#ff5400, #be1d00);" +
                        "-fx-background-radius: 30;" +
                        "-fx-background-inset: 0;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0.0, 0, 1);"
        );


        backButton.setStyle("-fx-background-color: red");
        backButton.setOnAction(e -> {
            primaryStage.setScene(sceneMap.get("main"));
            selectedUser = null;
        });

        // Header area
        // Header area
        Label opponentLabel = new Label("Opponent: ");
        opponentLabel.setStyle("-fx-font-size: 16px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");

        directMessageLabel = new Label(selectedUser);
        directMessageLabel.setStyle("-fx-font-size: 16px; -fx-font-family: 'Impact'; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.8) , 5, 0.0 , 0 , 1 );" +
                "-fx-stroke: black; -fx-stroke-width: 10;");
        HBox.setMargin(backButton, new Insets(10, 30, 0, 0));


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox serverNameHeader = new HBox(opponentLabel,directMessageLabel, spacer, backButton);
        serverNameHeader.setAlignment(Pos.CENTER);

        HBox opponentAndName = new HBox(opponentLabel,directMessageLabel);
        opponentAndName.setAlignment(Pos.CENTER);

        VBox messageAreaWithLabel = new VBox(5); // 5 pixels space between components
        messageAreaWithLabel.getChildren().addAll(opponentAndName, messageBox);
        messageAreaWithLabel.setAlignment(Pos.CENTER);

        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setLeft(new HBox(shipPanel, gridsBox)); // Include ships panel next to the grids
        mainLayout.setCenter(messageAreaWithLabel);
        mainLayout.setTop(serverNameHeader);
        mainLayout.setBackground(backgroundObj);

        return new Scene(mainLayout, 800, 700); // Adjusted scene size for extra content
    }
}
