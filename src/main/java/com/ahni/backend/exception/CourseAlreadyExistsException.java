package com.ahni.backend.exception;
public class CourseAlreadyExistsException extends RuntimeException {
    public CourseAlreadyExistsException() { super("같은 과목 코드가 이미 있습니다. 비활성 과목도 확인해 주세요."); }
}
