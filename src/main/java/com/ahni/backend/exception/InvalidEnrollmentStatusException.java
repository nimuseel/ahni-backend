package com.ahni.backend.exception;

public class InvalidEnrollmentStatusException extends RuntimeException {
    public InvalidEnrollmentStatusException() {
        super("재학 또는 휴학 상태만 선택할 수 있습니다.");
    }
}
