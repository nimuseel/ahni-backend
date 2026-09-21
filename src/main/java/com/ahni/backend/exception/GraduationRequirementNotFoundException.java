package com.ahni.backend.exception;

public class GraduationRequirementNotFoundException extends RuntimeException {
    public GraduationRequirementNotFoundException() {
        super("학생의 입학연도와 전공에 맞는 졸업요건을 찾을 수 없습니다.");
    }
}
