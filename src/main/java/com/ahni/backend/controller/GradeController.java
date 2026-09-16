package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.GradeRegistrationRequest;
import com.ahni.backend.dto.GradeResponse;
import com.ahni.backend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {
    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Operation(
        summary = "내 성적 등록",
        description = "[인증 O] JWT의 사용자에게 과목별 학기 성적을 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GradeRegistrationRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "courseEntityId": "00000000-0000-0000-0000-000000000101",
                      "academicYear": 2025,
                      "term": "SECOND",
                      "gradeCode": "A_PLUS",
                      "credit": 3.0,
                      "rpl": false,
                      "retake": false
                    }
                    """)
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "성적 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GradeResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "entityId": "00000000-0000-0000-0000-000000000201",
                      "course": {
                        "entityId": "00000000-0000-0000-0000-000000000101",
                        "code": "CSE101",
                        "name": "프로그래밍 기초",
                        "category": "MAJOR",
                        "department": {
                          "entityId": "00000000-0000-0000-0000-000000000001",
                          "name": "소프트웨어융합공학과"
                        }
                      },
                      "academicYear": 2025,
                      "term": "SECOND",
                      "gradeCode": "A_PLUS",
                      "gradePoint": 4.50,
                      "credit": 3.0,
                      "rpl": false,
                      "retake": false,
                      "createdAt": "2026-09-16T00:00:00Z",
                      "updatedAt": "2026-09-16T00:00:00Z"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "요청값 또는 성적 규칙 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "요청값 오류", value = """
                        {"code":"INVALID_REQUEST","message":"요청값이 올바르지 않습니다."}
                        """),
                    @ExampleObject(name = "성적 규칙 오류", value = """
                        {"code":"INVALID_GRADE","message":"일반 성적에는 등급이 필수입니다."}
                        """)
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "학생 프로필 또는 과목을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "학생 없음", value = """
                        {"code":"STUDENT_NOT_FOUND","message":"학생 프로필을 찾을 수 없습니다."}
                        """),
                    @ExampleObject(name = "과목 없음", value = """
                        {"code":"COURSE_NOT_FOUND","message":"과목을 찾을 수 없습니다."}
                        """)
                }
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "같은 학기의 과목 성적이 이미 등록됨",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"GRADE_ALREADY_REGISTERED","message":"해당 학기의 과목 성적이 이미 등록되어 있습니다."}
                    """)
            )
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse register(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody GradeRegistrationRequest request
    ) {
        return gradeService.register(UUID.fromString(jwt.getSubject()), request);
    }

    @Operation(
        summary = "내 성적 목록 조회",
        description = "[인증 O] JWT의 사용자에게 등록된 성적을 최신 학기순으로 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "성적 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = GradeResponse.class)),
                examples = @ExampleObject(value = """
                    [
                      {
                        "entityId": "00000000-0000-0000-0000-000000000201",
                        "course": {
                          "entityId": "00000000-0000-0000-0000-000000000101",
                          "code": "CSE101",
                          "name": "프로그래밍 기초",
                          "category": "MAJOR",
                          "department": {
                            "entityId": "00000000-0000-0000-0000-000000000001",
                            "name": "소프트웨어융합공학과"
                          }
                        },
                        "academicYear": 2025,
                        "term": "SECOND",
                        "gradeCode": "A_PLUS",
                        "gradePoint": 4.50,
                        "credit": 3.0,
                        "rpl": false,
                        "retake": false,
                        "createdAt": "2026-09-16T00:00:00Z",
                        "updatedAt": "2026-09-16T00:00:00Z"
                      }
                    ]
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "학생 프로필을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"STUDENT_NOT_FOUND","message":"학생 프로필을 찾을 수 없습니다."}
                    """)
            )
        )
    })
    @GetMapping
    public List<GradeResponse> getGrades(@AuthenticationPrincipal Jwt jwt) {
        return gradeService.getGrades(UUID.fromString(jwt.getSubject()));
    }
}
