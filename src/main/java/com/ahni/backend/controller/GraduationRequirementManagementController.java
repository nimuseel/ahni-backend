package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.GraduationRequirementCreateRequest;
import com.ahni.backend.dto.GraduationRequirementResponse;
import com.ahni.backend.dto.GraduationRequirementUpdateRequest;
import com.ahni.backend.service.GraduationRequirementManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/graduation-requirements")
public class GraduationRequirementManagementController {
    private final GraduationRequirementManagementService service;

    public GraduationRequirementManagementController(
        GraduationRequirementManagementService service
    ) {
        this.service = service;
    }

    @Operation(
        summary = "관리자용 졸업요건 목록 조회",
        description = "[관리자 인증 O] 학과, 입학연도, 전공 유형으로 졸업요건을 필터링합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "졸업요건 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
                {"code":"ADMIN_ACCESS_DENIED","message":"관리자 권한이 필요합니다."}
                """)
        ))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GraduationRequirementResponse> findAll(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) UUID departmentEntityId,
        @RequestParam(required = false) Integer admissionYear,
        @RequestParam(required = false) String majorType
    ) {
        return service.findAll(
            UUID.fromString(jwt.getSubject()),
            departmentEntityId,
            admissionYear,
            majorType
        );
    }

    @Operation(
        summary = "졸업요건 등록",
        description = "[관리자 인증 O] 졸업요건과 필수과목 구성을 함께 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "졸업요건 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
                {"code":"INVALID_REQUEST","message":"요청값이 올바르지 않습니다."}
                """)
        )),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
                {"code":"ADMIN_ACCESS_DENIED","message":"관리자 권한이 필요합니다."}
                """)
        )),
        @ApiResponse(responseCode = "404", description = "학과 또는 과목 없음", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class)
        )),
        @ApiResponse(responseCode = "409", description = "졸업요건 중복", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
                {"code":"GRADUATION_REQUIREMENT_ALREADY_EXISTS","message":"같은 학과, 입학연도, 전공 유형의 졸업요건이 이미 있습니다."}
                """)
        ))
    })
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public GraduationRequirementResponse create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody GraduationRequirementCreateRequest request
    ) {
        return service.create(UUID.fromString(jwt.getSubject()), request);
    }

    @Operation(
        summary = "졸업요건 수정",
        description = "[관리자 인증 O] 졸업요건의 학점, 출처, 필수과목 구성을 교체합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "졸업요건 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class)
        )),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
                {"code":"ADMIN_ACCESS_DENIED","message":"관리자 권한이 필요합니다."}
                """)
        )),
        @ApiResponse(responseCode = "404", description = "졸업요건 또는 과목 없음", content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class)
        ))
    })
    @PutMapping(
        value = "/{requirementEntityId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public GraduationRequirementResponse update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID requirementEntityId,
        @Valid @RequestBody GraduationRequirementUpdateRequest request
    ) {
        return service.update(
            UUID.fromString(jwt.getSubject()),
            requirementEntityId,
            request
        );
    }
}
