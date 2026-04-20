package server;

public class ServerMain {
    public static void main(String[] args) {
        ChatServer server = new ChatServer(5000);
        server.start();
    }
}