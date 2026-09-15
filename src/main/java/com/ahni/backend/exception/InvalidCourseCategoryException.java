package com.ahni.backend.exception;

public class InvalidCourseCategoryException extends RuntimeException {
    public InvalidCourseCategoryException() {
        super("과목 분류가 올바르지 않습니다.");
    }
}
