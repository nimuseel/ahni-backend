package com.ahni.backend.exception;

public class CurriculumEditConflictException extends RuntimeException {
    public CurriculumEditConflictException() { super("교과과정이 변경되었습니다. 다시 불러온 뒤 수정해 주세요."); }
}
