package com.ahni.backend.exception;

public class StudentEmailAlreadyRegisteredException extends RuntimeException {
    public StudentEmailAlreadyRegisteredException() {
        super("이 이메일로 등록된 학생 정보가 이미 있습니다.");
    }
}
