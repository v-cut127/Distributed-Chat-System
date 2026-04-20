package client;

import common.Command;
import common.CommandType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread listenerThread;
    private volatile boolean running = false;

    public void connect(String host, int port, ServerListener listener) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        running = true;

        listenerThread = new Thread(() -> {
            try {
                while (running) {
                    Object obj = in.readObject();
                    if (obj instanceof Command command) {
                        listener.onCommandReceived(command);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    listener.onDisconnected("Disconnected from server.");
                }
            } finally {
                disconnect();
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized void sendRegister(String username) throws IOException {
        Command cmd = new Command(CommandType.REGISTER);
        cmd.setUsername(username);
        sendCommand(cmd);
    }

    public synchronized void createRoom(String roomName) throws IOException {
        Command cmd = new Command(CommandType.CREATE_ROOM);
        cmd.setRoomName(roomName);
        sendCommand(cmd);
    }

    public synchronized void joinRoom(String roomName) throws IOException {
        Command cmd = new Command(CommandType.JOIN_ROOM);
        cmd.setRoomName(roomName);
        sendCommand(cmd);
    }

    public synchronized void leaveRoom() throws IOException {
        Command cmd = new Command(CommandType.LEAVE_ROOM);
        sendCommand(cmd);
    }

    public synchronized void sendMessage(String content) throws IOException {
        Command cmd = new Command(CommandType.SEND_MESSAGE);
        cmd.setContent(content);
        sendCommand(cmd);
    }

    private synchronized void sendCommand(Command cmd) throws IOException {
        out.writeObject(cmd);
        out.flush();
    }

    public void disconnect() {
        running = false;

        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}

        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}