package com.ahni.backend.controller;

import com.ahni.backend.dto.*;
import com.ahni.backend.service.CurriculumService;
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
@RequestMapping(value = "/api/v1/admin/curricula", produces = "application/json")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "입력 오류, 중복 연결 또는 빈 교과과정 공개", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"요청값이 올바르지 않습니다.\"}"))),
    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
    @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"ADMIN_ACCESS_DENIED\",\"message\":\"관리자 권한이 필요합니다.\"}"))),
    @ApiResponse(responseCode = "404", description = "교과과정·학과·과목 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = {
        @ExampleObject(name = "curriculum", value = "{\"code\":\"CURRICULUM_NOT_AVAILABLE\",\"message\":\"이 연도의 교과과정을 이용할 수 없습니다.\"}"),
        @ExampleObject(name = "department", value = "{\"code\":\"DEPARTMENT_NOT_FOUND\",\"message\":\"학과를 찾을 수 없습니다.\"}"),
        @ExampleObject(name = "course", value = "{\"code\":\"COURSE_NOT_FOUND\",\"message\":\"과목을 찾을 수 없습니다.\"}")
    })),
    @ApiResponse(responseCode = "409", description = "학과·연도 중복 또는 편집 버전 충돌", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = {
        @ExampleObject(name = "duplicate", value = "{\"code\":\"CURRICULUM_ALREADY_EXISTS\",\"message\":\"같은 학과와 연도의 교과과정이 이미 있습니다.\"}"),
        @ExampleObject(name = "version", value = "{\"code\":\"CURRICULUM_EDIT_CONFLICT\",\"message\":\"교과과정이 변경되었습니다. 다시 불러온 뒤 수정해 주세요.\"}")
    }))
})
public class AdminCurriculumController {
    static final String RESPONSE_EXAMPLE = """
        {"entityId":"00000000-0000-0000-0000-000000000501","version":0,"department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"},"curriculumYear":2024,"sourceTitle":"2024 교과과정표","sourceUrl":null,"published":false,"courses":[]}
        """;
    static final String CONNECTED_COURSES_EXAMPLE = """
        [{"entityId":"00000000-0000-0000-0000-000000000601","course":{"entityId":"00000000-0000-0000-0000-000000000101","code":"CSE101","name":"프로그래밍 기초","credit":3.0,"category":"MAJOR","department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"}},"division":"MAJOR_REQUIRED","recommendedYear":1,"recommendedTerm":"FIRST","areaCode":null,"areaName":null,"majorArea":null,"note":null}]
        """;
    static final String CREATED_EXAMPLE = """
        {"entityId":"00000000-0000-0000-0000-000000000501","version":0,"department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"},"curriculumYear":2024,"sourceTitle":"2024 교과과정표","sourceUrl":null,"published":false,"courses":
        """ + CONNECTED_COURSES_EXAMPLE + "}";
    static final String UPDATED_EXAMPLE = """
        {"entityId":"00000000-0000-0000-0000-000000000501","version":2,"department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"},"curriculumYear":2024,"sourceTitle":"2024 교과과정표 정정","sourceUrl":null,"published":false,"courses":[]}
        """;
    static final String PUBLISHED_EXAMPLE = """
        {"entityId":"00000000-0000-0000-0000-000000000501","version":1,"department":{"entityId":"00000000-0000-0000-0000-000000000001","name":"소프트웨어융합공학과"},"curriculumYear":2024,"sourceTitle":"2024 교과과정표","sourceUrl":null,"published":true,"courses":
        """ + CONNECTED_COURSES_EXAMPLE + "}";
    private final CurriculumService service;
    public AdminCurriculumController(CurriculumService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "관리자 교과과정 목록", description = "[관리자 인증 O] 학과명·연도순 초안 및 공개 자료를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "목록 조회 성공", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CurriculumResponse.class)), examples = @ExampleObject(value = "[" + RESPONSE_EXAMPLE + "]")))
    public List<CurriculumResponse> list(@AuthenticationPrincipal Jwt jwt) { return service.list(subject(jwt)); }

    @GetMapping("/{entityId}")
    @Operation(summary = "관리자 교과과정 상세", description = "[관리자 인증 O] 편집 버전과 과목 연결을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "상세 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CurriculumResponse.class), examples = @ExampleObject(value = RESPONSE_EXAMPLE)))
    public CurriculumResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entityId) { return service.get(subject(jwt), entityId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "교과과정 초안 생성", description = "[관리자 인증 O] 기존 과목을 연도별로 연결합니다. 빈 초안은 허용하며 version은 null입니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurriculumRequest.class), examples = @ExampleObject(value = """
            {"departmentEntityId":"00000000-0000-0000-0000-000000000001","curriculumYear":2024,"sourceTitle":"2024 교과과정표","sourceUrl":null,"version":null,"courses":[{"courseEntityId":"00000000-0000-0000-0000-000000000101","division":"MAJOR_REQUIRED","recommendedYear":1,"recommendedTerm":"FIRST","areaCode":null,"areaName":null,"majorArea":null,"note":null}]}
            """))))
    @ApiResponse(responseCode = "201", description = "초안 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CurriculumResponse.class), examples = @ExampleObject(value = CREATED_EXAMPLE)))
    public CurriculumResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CurriculumRequest input) { return service.create(subject(jwt), input); }

    @PutMapping("/{entityId}")
    @Operation(summary = "교과과정 전체 수정", description = "[관리자 인증 O] 학과·연도는 유지하고 출처·연결 전체를 원자적으로 교체합니다. 현재 version은 필수이며 저장 후 비공개 초안입니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurriculumRequest.class), examples = @ExampleObject(value = """
            {"departmentEntityId":"00000000-0000-0000-0000-000000000001","curriculumYear":2024,"sourceTitle":"2024 교과과정표 정정","sourceUrl":null,"version":1,"courses":[]}
            """))))
    @ApiResponse(responseCode = "200", description = "수정 성공. 버전 증가, 비공개 초안", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CurriculumResponse.class), examples = @ExampleObject(value = UPDATED_EXAMPLE)))
    public CurriculumResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entityId, @Valid @RequestBody CurriculumRequest input) { return service.update(subject(jwt), entityId, input); }

    @PutMapping("/{entityId}/publication")
    @Operation(summary = "교과과정 공개 상태 변경", description = "[관리자 인증 O] 검토한 자료만 공개합니다. 빈 자료 공개는 거절하며 현재 version은 필수입니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurriculumPublicationRequest.class), examples = @ExampleObject(value = "{\"published\":true,\"version\":0}"))))
    @ApiResponse(responseCode = "200", description = "공개 상태 변경 성공. 변경 시 버전 증가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CurriculumResponse.class), examples = @ExampleObject(value = PUBLISHED_EXAMPLE)))
    public CurriculumResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entityId, @Valid @RequestBody CurriculumPublicationRequest input) { return service.publish(subject(jwt), entityId, input); }

    private static UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
