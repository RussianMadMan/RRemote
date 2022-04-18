package ru.rmm.server.ca;

public class CAException extends Exception {
    public CAException(String message, Throwable cause){
        super(message, cause);
    }
    public CAException(String message){
        super(message);
    }
}
