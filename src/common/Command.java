package common;

import java.io.Serializable;
import java.util.List;

public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandType type;
    private String username;
    private String roomName;
    private String content;
    private String message;
    private List<String> roomList;
    private Message chatMessage;

    public Command(CommandType type) {
        this.type = type;
    }

    public CommandType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getContent() {
        return content;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getRoomList() {
        return roomList;
    }

    public Message getChatMessage() {
        return chatMessage;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRoomList(List<String> roomList) {
        this.roomList = roomList;
    }

    public void setChatMessage(Message chatMessage) {
        this.chatMessage = chatMessage;
    }
}