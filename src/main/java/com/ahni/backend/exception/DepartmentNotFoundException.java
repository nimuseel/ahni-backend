package com.ahni.backend.exception;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException() {
        super("학과를 찾을 수 없습니다.");
    }
}
