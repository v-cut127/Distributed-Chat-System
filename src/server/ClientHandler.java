package server;

import common.Command;
import common.CommandType;
import common.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String username;
    private ChatRoom currentRoom;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object obj = in.readObject();

                if (obj instanceof Command command) {
                    handleCommand(command);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + username);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleCommand(Command command) {
        switch (command.getType()) {
            case REGISTER -> handleRegister(command);
            case CREATE_ROOM -> handleCreateRoom(command);
            case JOIN_ROOM -> handleJoinRoom(command);
            case LEAVE_ROOM -> handleLeaveRoom();
            case SEND_MESSAGE -> handleSendMessage(command);
            default -> sendError("Unknown command.");
        }
    }

    private void handleRegister(Command command) {
        String requestedUsername = command.getUsername();

        if (requestedUsername == null || requestedUsername.isBlank()) {
            sendError("Username cannot be empty.");
            return;
        }

        boolean success = server.registerClient(requestedUsername, this);
        if (!success) {
            sendError("Username already exists.");
            return;
        }

        this.username = requestedUsername;

        Command response = new Command(CommandType.SUCCESS);
        response.setMessage("Registered successfully as " + username);
        sendCommand(response);

        sendRoomList();
    }

    private void handleCreateRoom(Command command) {
        if (!isRegistered()) return;

        String roomName = command.getRoomName();
        if (roomName == null || roomName.isBlank()) {
            sendError("Room name cannot be empty.");
            return;
        }

        boolean created = server.createRoom(roomName);
        if (!created) {
            sendError("Room already exists.");
            return;
        }

        Command response = new Command(CommandType.SUCCESS);
        response.setMessage("Room created: " + roomName);
        sendCommand(response);

        sendRoomList();
    }

    private void handleJoinRoom(Command command) {
        if (!isRegistered()) return;

        String roomName = command.getRoomName();
        ChatRoom room = server.getRoom(roomName);

        if (room == null) {
            sendError("Room does not exist.");
            return;
        }

        if (currentRoom != null) {
            currentRoom.removeMember(this);
        }

        currentRoom = room;
        currentRoom.addMember(this);

        Command response = new Command(CommandType.SUCCESS);
        response.setMessage("Joined room: " + roomName);
        sendCommand(response);
    }

    private void handleLeaveRoom() {
        if (!isRegistered()) return;

        if (currentRoom == null) {
            sendError("You are not in any room.");
            return;
        }

        currentRoom.removeMember(this);
        String leftRoomName = currentRoom.getRoomName();
        currentRoom = null;

        Command response = new Command(CommandType.SUCCESS);
        response.setMessage("Left room: " + leftRoomName);
        sendCommand(response);
    }

    private void handleSendMessage(Command command) {
        if (!isRegistered()) return;

        if (currentRoom == null) {
            sendError("Join a room first.");
            return;
        }

        String content = command.getContent();
        if (content == null || content.isBlank()) {
            sendError("Message cannot be empty.");
            return;
        }

        Message message = new Message(username, content, currentRoom.getRoomName());
        currentRoom.enqueueMessage(message);
    }

    private boolean isRegistered() {
        if (username == null) {
            sendError("You must register first.");
            return false;
        }
        return true;
    }

    public synchronized void sendChatMessage(Message message) {
        try {
            Command cmd = new Command(CommandType.SEND_MESSAGE);
            cmd.setChatMessage(message);
            out.writeObject(cmd);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending chat message: " + e.getMessage());
        }
    }

    public synchronized void sendCommand(Command command) {
        try {
            out.writeObject(command);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending command: " + e.getMessage());
        }
    }

    private void sendError(String text) {
        Command cmd = new Command(CommandType.ERROR);
        cmd.setMessage(text);
        sendCommand(cmd);
    }

    private void sendRoomList() {
        Command cmd = new Command(CommandType.ROOM_LIST);
        cmd.setRoomList(server.getRoomNames());
        sendCommand(cmd);
    }

    private void cleanup() {
        try {
            if (currentRoom != null) {
                currentRoom.removeMember(this);
            }

            server.removeClient(username);

            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Cleanup error: " + e.getMessage());
        }
    }
}