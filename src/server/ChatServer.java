package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;

    private final Map<String, ClientHandler> clients = new HashMap<>();
    private final Map<String, ChatRoom> rooms = new HashMap<>();

    private final ReentrantLock clientsLock = new ReentrantLock();
    private final ReentrantLock roomsLock = new ReentrantLock();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(socket, this);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public boolean registerClient(String username, ClientHandler handler) {
        clientsLock.lock();
        try {
            if (clients.containsKey(username)) {
                return false;
            }
            clients.put(username, handler);
            return true;
        } finally {
            clientsLock.unlock();
        }
    }

    public void removeClient(String username) {
        if (username == null) return;

        clientsLock.lock();
        try {
            clients.remove(username);
        } finally {
            clientsLock.unlock();
        }
    }

    public boolean createRoom(String roomName) {
        roomsLock.lock();
        try {
            if (rooms.containsKey(roomName)) {
                return false;
            }

            ChatRoom room = new ChatRoom(roomName);
            rooms.put(roomName, room);

            Thread roomThread = new Thread(room);
            roomThread.start();

            return true;
        } finally {
            roomsLock.unlock();
        }
    }

    public ChatRoom getRoom(String roomName) {
        roomsLock.lock();
        try {
            return rooms.get(roomName);
        } finally {
            roomsLock.unlock();
        }
    }

    public List<String> getRoomNames() {
        roomsLock.lock();
        try {
            return new ArrayList<>(rooms.keySet());
        } finally {
            roomsLock.unlock();
        }
    }
}