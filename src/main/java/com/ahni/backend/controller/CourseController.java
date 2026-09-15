package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(
        summary = "과목 목록 조회",
        description = "[인증 O] 활성 과목을 조회하며 학과와 과목 분류로 필터링할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "과목 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CourseResponse.class)),
                examples = @ExampleObject(value = """
                    [
                      {
                        "entityId": "00000000-0000-0000-0000-000000000101",
                        "code": "CSE101",
                        "name": "프로그래밍 기초",
                        "credit": 3.0,
                        "category": "MAJOR",
                        "department": {
                          "entityId": "00000000-0000-0000-0000-000000000001",
                          "name": "소프트웨어융합공학과"
                        }
                      },
                      {
                        "entityId": "00000000-0000-0000-0000-000000000102",
                        "code": "GE101",
                        "name": "대학 글쓰기",
                        "credit": 2.0,
                        "category": "GENERAL_EDUCATION",
                        "department": null
                      }
                    ]
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "요청값 또는 과목 분류 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "요청값 오류",
                        value = """
                            {"code":"INVALID_REQUEST","message":"요청값이 올바르지 않습니다."}
                            """
                    ),
                    @ExampleObject(
                        name = "과목 분류 오류",
                        value = """
                            {"code":"INVALID_COURSE_CATEGORY","message":"과목 분류가 올바르지 않습니다."}
                            """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "학과를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"DEPARTMENT_NOT_FOUND","message":"학과를 찾을 수 없습니다."}
                    """)
            )
        )
    })
    @GetMapping
    public List<CourseResponse> getCourses(
        @Parameter(description = "학과 식별자")
        @RequestParam(required = false) UUID departmentEntityId,
        @Parameter(
            description = "과목 분류",
            example = "MAJOR",
            schema = @Schema(allowableValues = {"MAJOR", "GENERAL_EDUCATION", "ELECTIVE"})
        )
        @RequestParam(required = false) String category
    ) {
        return courseService.getCourses(departmentEntityId, category);
    }
}
