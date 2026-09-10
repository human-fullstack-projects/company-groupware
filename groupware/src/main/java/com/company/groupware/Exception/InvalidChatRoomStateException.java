package com.company.groupware.Exception;

public class InvalidChatRoomStateException extends RuntimeException{
    public InvalidChatRoomStateException(String message) {
        super(message);
    }
}
