package com.ahni.backend.exception;

public class CourseNotInCurriculumException extends RuntimeException {
    public CourseNotInCurriculumException() { super("선택한 수강 연도의 교과과정에 없는 과목입니다."); }
}
