package com.ahni.backend.exception;
public class CourseAssignmentConflictException extends RuntimeException {
    public CourseAssignmentConflictException() { super("졸업요건 또는 교과과정에 연결된 과목의 분류와 학과는 변경할 수 없습니다. 연결을 먼저 확인해 주세요."); }
}
