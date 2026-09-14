package com.ahni.backend.exception;

public class DuplicateMajorDepartmentException extends RuntimeException {
    public DuplicateMajorDepartmentException() {
        super("같은 학과를 여러 전공 유형으로 선택할 수 없습니다.");
    }
}
