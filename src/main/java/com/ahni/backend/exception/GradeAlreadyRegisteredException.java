package com.ahni.backend.exception;

public class GradeAlreadyRegisteredException extends RuntimeException {
    public GradeAlreadyRegisteredException() {
        super("해당 학기의 과목 성적이 이미 등록되어 있습니다.");
    }
}
