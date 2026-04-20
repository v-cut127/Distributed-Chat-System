package common;

import java.io.Serializable;

public enum CommandType implements Serializable {
    REGISTER,
    CREATE_ROOM,
    JOIN_ROOM,
    LEAVE_ROOM,
    SEND_MESSAGE,
    SUCCESS,
    ERROR,
    SYSTEM_MESSAGE,
    ROOM_LIST
}