package server;

import common.Command;
import common.CommandType;
import common.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ChatRoom implements Runnable {
    private String roomName;
    private final List<ClientHandler> members = new ArrayList<>();
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock membersLock = new ReentrantLock();
    private volatile boolean running = true;

    public ChatRoom(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void addMember(ClientHandler client) {
        membersLock.lock();
        try {
            if (!members.contains(client)) {
                members.add(client);
            }
        } finally {
            membersLock.unlock();
        }

        broadcastSystemMessage(client.getUsername() + " joined room " + roomName);
    }

    public void removeMember(ClientHandler client) {
        membersLock.lock();
        try {
            members.remove(client);
        } finally {
            membersLock.unlock();
        }

        broadcastSystemMessage(client.getUsername() + " left room " + roomName);
    }

    public void enqueueMessage(Message message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<String> getMemberNames() {
        membersLock.lock();
        try {
            List<String> names = new ArrayList<>();
            for (ClientHandler member : members) {
                names.add(member.getUsername());
            }
            return names;
        } finally {
            membersLock.unlock();
        }
    }

    private void broadcast(Message message) {
        membersLock.lock();
        try {
            for (ClientHandler member : members) {
                member.sendChatMessage(message);
            }
        } finally {
            membersLock.unlock();
        }
    }

    private void broadcastSystemMessage(String text) {
        Command cmd = new Command(CommandType.SYSTEM_MESSAGE);
        cmd.setMessage(text);

        membersLock.lock();
        try {
            for (ClientHandler member : members) {
                member.sendCommand(cmd);
            }
        } finally {
            membersLock.unlock();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Message message = messageQueue.take();
                broadcast(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}