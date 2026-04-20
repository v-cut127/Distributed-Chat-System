package gui;

import client.ChatClient;
import client.ServerListener;
import common.Command;
import common.CommandType;
import common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ChatApp extends Application {

    private Stage primaryStage;
    private ChatClient client;

    private String currentUsername = null;
    private String currentRoom = null;

    private TextArea chatArea;
    private ListView<String> roomListView;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Distributed Chat Client");
        showLoginScene();
        primaryStage.show();
    }

    private void showLoginScene() {
        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField("localhost");

        Label portLabel = new Label("Port:");
        TextField portField = new TextField("5000");

        Label userLabel = new Label("Username:");
        TextField userField = new TextField();

        Label infoLabel = new Label();

        Button connectBtn = new Button("Connect");

        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String username = userField.getText().trim();

            if (host.isEmpty() || portText.isEmpty() || username.isEmpty()) {
                infoLabel.setText("Fill all fields.");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                infoLabel.setText("Port must be a number.");
                return;
            }

            try {
                client = new ChatClient();
                client.connect(host, port, new ServerListener() {
                    @Override
                    public void onCommandReceived(Command command) {
                        Platform.runLater(() -> handleServerCommand(command));
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, reason, ButtonType.OK);
                            alert.showAndWait();
                            showLoginScene();
                        });
                    }
                });

                client.sendRegister(username);
                currentUsername = username;
                showChatScene();
            } catch (Exception ex) {
                infoLabel.setText("Connection failed: " + ex.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        form.add(hostLabel, 0, 0);
        form.add(hostField, 1, 0);
        form.add(portLabel, 0, 1);
        form.add(portField, 1, 1);
        form.add(userLabel, 0, 2);
        form.add(userField, 1, 2);

        VBox root = new VBox(15, form, connectBtn, infoLabel);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 250);
        primaryStage.setScene(scene);
    }

    private void showChatScene() {
        roomListView = new ListView<>();
        roomListView.setPrefWidth(180);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        statusLabel = new Label("Connected as: " + currentUsername);

        TextField roomField = new TextField();
        roomField.setPromptText("Room name");

        Button createRoomBtn = new Button("Create Room");
        Button joinRoomBtn = new Button("Join Selected");
        Button leaveRoomBtn = new Button("Leave Room");

        createRoomBtn.setOnAction(e -> {
            String roomName = roomField.getText().trim();
            if (roomName.isEmpty()) {
                appendSystem("Enter a room name first.");
                return;
            }

            try {
                client.createRoom(roomName);
                roomField.clear();
            } catch (Exception ex) {
                appendSystem("Failed to create room.");
            }
        });

        joinRoomBtn.setOnAction(e -> {
            String selected = roomListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                appendSystem("Select a room first.");
                return;
            }

            try {
                client.joinRoom(selected);
                currentRoom = selected;
                updateStatus();
            } catch (Exception ex) {
                appendSystem("Failed to join room.");
            }
        });

        leaveRoomBtn.setOnAction(e -> {
            try {
                client.leaveRoom();
                currentRoom = null;
                updateStatus();
            } catch (Exception ex) {
                appendSystem("Failed to leave room.");
            }
        });

        VBox roomBox = new VBox(10,
                new Label("Rooms"),
                roomListView,
                roomField,
                createRoomBtn,
                joinRoomBtn,
                leaveRoomBtn
        );
        roomBox.setPadding(new Insets(10));
        roomBox.setPrefWidth(200);

        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");

        Button sendBtn = new Button("Send");

        Runnable sendAction = () -> {
            String text = messageField.getText().trim();
            if (text.isEmpty()) return;

            try {
                client.sendMessage(text);
                messageField.clear();
            } catch (Exception ex) {
                appendSystem("Failed to send message.");
            }
        };

        sendBtn.setOnAction(e -> sendAction.run());
        messageField.setOnAction(e -> sendAction.run());

        HBox messageBox = new HBox(10, messageField, sendBtn);
        messageBox.setPadding(new Insets(10));
        HBox.setHgrow(messageField, Priority.ALWAYS);

        BorderPane centerPane = new BorderPane();
        centerPane.setCenter(chatArea);
        centerPane.setBottom(messageBox);
        centerPane.setTop(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(roomBox);
        root.setCenter(centerPane);

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setScene(scene);
    }

    private void handleServerCommand(Command command) {
        if (command.getType() == CommandType.ERROR) {
            appendSystem("ERROR: " + command.getMessage());
            return;
        }

        if (command.getType() == CommandType.SUCCESS) {
            appendSystem(command.getMessage());
            return;
        }

        if (command.getType() == CommandType.SYSTEM_MESSAGE) {
            appendSystem(command.getMessage());
            return;
        }

        if (command.getType() == CommandType.ROOM_LIST) {
            roomListView.setItems(FXCollections.observableArrayList(command.getRoomList()));
            return;
        }

        if (command.getType() == CommandType.SEND_MESSAGE) {
            Message msg = command.getChatMessage();
            if (msg != null) {
                chatArea.appendText(msg.format() + "\n");
            }
        }
    }

    private void appendSystem(String text) {
        if (chatArea != null) {
            chatArea.appendText("[SYSTEM] " + text + "\n");
        }
    }

    private void updateStatus() {
        if (statusLabel != null) {
            if (currentRoom == null) {
                statusLabel.setText("Connected as: " + currentUsername + " | No room joined");
            } else {
                statusLabel.setText("Connected as: " + currentUsername + " | Room: " + currentRoom);
            }
        }
    }

    @Override
    public void stop() {
        if (client != null) {
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}