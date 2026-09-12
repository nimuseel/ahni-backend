package com.ahni.backend.controller;

import com.ahni.backend.dto.StudentProfileRegistrationRequest;
import com.ahni.backend.dto.StudentProfileResponse;
import com.ahni.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(
        summary = "내 학생 프로필 조회",
        description = "[인증 O] JWT의 사용자 식별자로 학생 프로필과 주전공을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "학생 프로필 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StudentProfileResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "studentEntityId": "00000000-0000-0000-0000-000000000020",
                      "email": "student@inha.edu",
                      "nickname": "인하",
                      "primaryDepartment": {
                        "entityId": "00000000-0000-0000-0000-000000000001",
                        "name": "소프트웨어융합공학과"
                      },
                      "admissionYear": 2024,
                      "enrollmentStatus": "ENROLLED",
                      "accountStatus": "ACTIVE"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "등록된 학생 프로필이 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.ahni.backend.dto.ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"STUDENT_NOT_FOUND","message":"학생 프로필을 찾을 수 없습니다."}
                    """)
            )
        )
    })
    @GetMapping("/me")
    public StudentProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return studentService.getProfile(UUID.fromString(jwt.getSubject()));
    }

    @Operation(
        summary = "학생 프로필 등록",
        description = "[인증 O] JWT의 사용자 정보로 학생 프로필과 주전공을 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StudentProfileRegistrationRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "primaryDepartmentEntityId": "00000000-0000-0000-0000-000000000001",
                      "admissionYear": 2024,
                      "enrollmentStatus": "ENROLLED",
                      "nickname": "인하"
                    }
                    """)
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "학생 프로필 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StudentProfileResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "studentEntityId": "00000000-0000-0000-0000-000000000020",
                      "email": "student@inha.edu",
                      "nickname": "인하",
                      "primaryDepartment": {
                        "entityId": "00000000-0000-0000-0000-000000000001",
                        "name": "소프트웨어융합공학과"
                      },
                      "admissionYear": 2024,
                      "enrollmentStatus": "ENROLLED",
                      "accountStatus": "ACTIVE"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "요청값 또는 재학 상태 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.ahni.backend.dto.ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "요청값 오류", value = """
                        {"code":"INVALID_REQUEST","message":"요청값이 올바르지 않습니다."}
                        """),
                    @ExampleObject(name = "재학 상태 오류", value = """
                        {"code":"INVALID_ENROLLMENT_STATUS","message":"재학 또는 휴학 상태만 선택할 수 있습니다."}
                        """)
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "학과를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.ahni.backend.dto.ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"DEPARTMENT_NOT_FOUND","message":"학과를 찾을 수 없습니다."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "이미 등록된 학생",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.ahni.backend.dto.ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"STUDENT_ALREADY_REGISTERED","message":"이미 가입된 학생입니다."}
                    """)
            )
        )
    })
    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfileResponse registerProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody StudentProfileRegistrationRequest request
    ) {
        return studentService.registerProfile(
            UUID.fromString(jwt.getSubject()),
            jwt.getClaimAsString("email"),
            request
        );
    }
}
