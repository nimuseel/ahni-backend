package com.ahni.backend.exception;

public class GraduationRequirementAlreadyExistsException extends RuntimeException {
    public GraduationRequirementAlreadyExistsException() {
        super("같은 학과, 입학연도, 전공 유형의 졸업요건이 이미 있습니다.");
    }
}
