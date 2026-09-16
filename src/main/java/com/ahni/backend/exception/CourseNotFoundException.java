package com.ahni.backend.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException() {
        super("과목을 찾을 수 없습니다.");
    }
}
