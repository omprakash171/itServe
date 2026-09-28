package com.itserve.exception;

public class InvalidTicketStatusException extends RuntimeException {
    public InvalidTicketStatusException(String message) {
        super(message);
    }
}
