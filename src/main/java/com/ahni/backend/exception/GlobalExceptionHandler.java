package com.ahni.backend.exception;

import com.ahni.backend.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {
    @ExceptionHandler(StudentAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleStudentAlreadyRegistered(StudentAlreadyRegisteredException exception) {
        return new ApiErrorResponse("STUDENT_ALREADY_REGISTERED", exception.getMessage());
    }

    @ExceptionHandler(StudentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleStudentNotFound(StudentNotFoundException exception) {
        return new ApiErrorResponse("STUDENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDepartmentNotFound(DepartmentNotFoundException exception) {
        return new ApiErrorResponse("DEPARTMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidEnrollmentStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidEnrollmentStatus(InvalidEnrollmentStatusException exception) {
        return new ApiErrorResponse("INVALID_ENROLLMENT_STATUS", exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidRequest() {
        return new ApiErrorResponse("INVALID_REQUEST", "요청값이 올바르지 않습니다.");
    }
}
