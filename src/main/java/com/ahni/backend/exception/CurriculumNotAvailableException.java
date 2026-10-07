package com.ahni.backend.exception;

public class CurriculumNotAvailableException extends RuntimeException {
    public CurriculumNotAvailableException() { super("이 연도의 교과과정을 이용할 수 없습니다."); }
}
