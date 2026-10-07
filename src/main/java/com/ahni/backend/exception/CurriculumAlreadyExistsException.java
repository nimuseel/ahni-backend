package com.ahni.backend.exception;

public class CurriculumAlreadyExistsException extends RuntimeException {
    public CurriculumAlreadyExistsException() { super("같은 학과와 연도의 교과과정이 이미 있습니다."); }
}
