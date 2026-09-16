package com.ahni.backend.exception;

public class InvalidGradeException extends RuntimeException {
    public InvalidGradeException(String message) {
        super(message);
    }
}
