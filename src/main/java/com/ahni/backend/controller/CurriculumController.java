package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.service.CurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/curriculum-courses", produces = "application/json")
public class CurriculumController {
    private final CurriculumService service;
    public CurriculumController(CurriculumService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "수강 연도별 교과과정 과목", description = "[인증 O] 선택 연도의 공개 교과과정만 조회합니다. 학과 필터는 교과과정 학과에 적용합니다. 같은 과목은 한 번만 반환하며 과거 비활성 과목도 명시적으로 연결되었으면 포함합니다. 권장 학년·학기로 제한하거나 다른 연도로 대체하지 않습니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "학수번호순 과목 목록", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponse.class)), examples = @ExampleObject(value = """
            [{"entityId":"00000000-0000-0000-0000-000000000101","code":"ITC1201","name":"컴퓨터공학기초","credit":3.0,"category":"MAJOR","department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"}}]
            """))),
        @ApiResponse(responseCode = "400", description = "연도 누락/범위 오류 또는 잘못된 학과 식별자", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"))),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "404", description = "공개 교과과정 또는 학과 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = {
            @ExampleObject(name = "curriculum", value = "{\"code\":\"CURRICULUM_NOT_AVAILABLE\",\"message\":\"이 연도의 교과과정을 이용할 수 없습니다.\"}"),
            @ExampleObject(name = "department", value = "{\"code\":\"DEPARTMENT_NOT_FOUND\",\"message\":\"학과를 찾을 수 없습니다.\"}")
        }))
    })
    public List<CourseResponse> getCourses(
        @RequestParam @Min(2000) @Max(9999) int academicYear,
        @RequestParam(required = false) UUID departmentEntityId
    ) { return service.getCourses(academicYear, departmentEntityId); }
}
