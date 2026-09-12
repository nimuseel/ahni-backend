package com.ahni.backend.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException() {
        super("학생 프로필을 찾을 수 없습니다.");
    }
}
