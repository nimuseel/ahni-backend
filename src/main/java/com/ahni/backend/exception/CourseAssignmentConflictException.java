package com.ahni.backend.exception;
public class CourseAssignmentConflictException extends RuntimeException {
    public CourseAssignmentConflictException() { super("졸업요건에 배정된 과목의 분류 또는 학과를 변경할 수 없습니다. 필수과목 배정을 먼저 확인해 주세요."); }
}
