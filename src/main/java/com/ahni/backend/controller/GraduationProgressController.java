package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.GraduationProgressResponse;
import com.ahni.backend.service.GraduationProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/graduation-progress")
public class GraduationProgressController {
    private final GraduationProgressService graduationProgressService;

    public GraduationProgressController(
        GraduationProgressService graduationProgressService
    ) {
        this.graduationProgressService = graduationProgressService;
    }

    @Operation(
        summary = "내 졸업요건 학점 충족도 조회",
        description = "[인증 O] 입학연도와 활성 전공별 전체·전공·교양 학점 충족도를 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "졸업요건 학점 충족도 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(
                    schema = @Schema(implementation = GraduationProgressResponse.class)
                ),
                examples = @ExampleObject(value = """
                    [
                      {
                        "requirementEntityId": "00000000-0000-0000-0000-000000000401",
                        "admissionYear": 2024,
                        "majorType": "PRIMARY",
                        "department": {
                          "entityId": "00000000-0000-0000-0000-000000000001",
                          "name": "소프트웨어융합공학과"
                        },
                        "credits": {
                          "total": {
                            "required": 130.0,
                            "completed": 42.0,
                            "remaining": 88.0,
                            "met": false
                          },
                          "department": {
                            "required": 60.0,
                            "completed": 24.0,
                            "remaining": 36.0,
                            "met": false
                          },
                          "general": {
                            "required": 30.0,
                            "completed": 12.0,
                            "remaining": 18.0,
                            "met": false
                          }
                        }
                      }
                    ]
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "학생 프로필 또는 적용 가능한 졸업요건을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "학생 없음", value = """
                        {"code":"STUDENT_NOT_FOUND","message":"학생 프로필을 찾을 수 없습니다."}
                        """),
                    @ExampleObject(name = "졸업요건 없음", value = """
                        {"code":"GRADUATION_REQUIREMENT_NOT_FOUND","message":"학생의 입학연도와 전공에 맞는 졸업요건을 찾을 수 없습니다."}
                        """)
                }
            )
        )
    })
    @GetMapping
    public List<GraduationProgressResponse> getProgress(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return graduationProgressService.getProgress(
            UUID.fromString(jwt.getSubject())
        );
    }
}
