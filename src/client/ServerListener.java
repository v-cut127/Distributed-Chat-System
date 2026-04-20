package client;

import common.Command;

public interface ServerListener {
    void onCommandReceived(Command command);
    void onDisconnected(String reason);
}