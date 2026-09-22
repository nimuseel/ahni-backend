package com.ahni.backend.exception;

public class InvalidRequiredCourseAssignmentException extends RuntimeException {
    public InvalidRequiredCourseAssignmentException(String message) {
        super(message);
    }
}
