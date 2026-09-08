package com.ahni.backend.exception;

public class StudentAlreadyRegisteredException extends RuntimeException {
    public StudentAlreadyRegisteredException() {
        super("이미 가입된 학생입니다.");
    }
}
