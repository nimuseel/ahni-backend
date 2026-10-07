package com.ahni.backend.controller;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.*;
import com.ahni.backend.service.CourseManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 입력", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"))),
    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
    @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"ADMIN_ACCESS_DENIED\",\"message\":\"관리자 권한이 필요합니다.\"}"))),
    @ApiResponse(responseCode = "404", description = "과목 또는 학과 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"COURSE_NOT_FOUND\",\"message\":\"과목을 찾을 수 없습니다.\"}"))),
    @ApiResponse(responseCode = "409", description = "과목 코드 중복 또는 졸업요건·교과과정 연결 충돌", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = {
        @ExampleObject(name = "duplicateCode", value = "{\"code\":\"COURSE_ALREADY_EXISTS\",\"message\":\"같은 과목 코드가 이미 있습니다. 비활성 과목도 확인해 주세요.\"}"),
        @ExampleObject(name = "assignmentConflict", value = "{\"code\":\"COURSE_ASSIGNMENT_CONFLICT\",\"message\":\"졸업요건 또는 교과과정의 과목 연결을 먼저 확인해 주세요.\"}")
    }))
})
public class CourseManagementController {
    private final CourseManagementService service;
    public CourseManagementController(CourseManagementService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "관리자 과목 목록", description = "[관리자 인증 O] 활성·비활성 과목을 코드순 조회합니다. 필터 생략 시 전체 목록입니다.")
    public List<AdminCourseResponse> findAll(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID departmentEntityId, @RequestParam(required = false) CourseCategory category, @RequestParam(required = false) Boolean active) {
        return service.findAll(UUID.fromString(jwt.getSubject()), departmentEntityId, category, active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "과목 등록", description = "[관리자 인증 O] 코드는 공백 제거 후 대문자로 저장합니다. 비활성 과목도 코드 중복 검사에 포함됩니다.")
    @ApiResponse(responseCode = "201", description = "과목 등록 성공")
    public AdminCourseResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseManagementRequest input) {
        return service.create(UUID.fromString(jwt.getSubject()), input);
    }

    @PutMapping("/{courseEntityId}")
    @Operation(summary = "과목 수정", description = "[관리자 인증 O] 외부 식별자 및 기존 성적의 학점 스냅샷은 유지합니다. 활성 필수과목 배정을 무효화하거나 교과과정에 연결된 과목의 학과·분류를 변경하는 수정은 거부합니다.")
    public AdminCourseResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseEntityId, @Valid @RequestBody CourseManagementRequest input) {
        return service.update(UUID.fromString(jwt.getSubject()), courseEntityId, input);
    }

    @PostMapping("/{courseEntityId}/deactivate")
    @Operation(summary = "과목 비활성화", description = "[관리자 인증 O] 일반 활성 목록에서는 숨기되 기존 성적과 졸업요건 배정은 보존합니다. 공개된 연도별 교과과정에 연결된 과목은 해당 연도 성적 입력에 계속 사용할 수 있습니다. 반복 요청은 동일한 비활성 상태를 반환합니다.")
    public AdminCourseResponse deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseEntityId) {
        return service.deactivate(UUID.fromString(jwt.getSubject()), courseEntityId);
    }
}
