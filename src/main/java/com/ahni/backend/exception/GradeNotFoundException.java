package com.ahni.backend.exception;

public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException() {
        super("성적을 찾을 수 없습니다.");
    }
}
