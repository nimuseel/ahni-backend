package com.ahni.backend.controller;

import com.ahni.backend.dto.ApiErrorResponse;
import com.ahni.backend.dto.GraduationRequirementImpactResponse;
import com.ahni.backend.service.GraduationRequirementImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/admin/graduation-requirements", produces = MediaType.APPLICATION_JSON_VALUE)
public class GraduationRequirementImpactController {
    private final GraduationRequirementImpactService service;

    public GraduationRequirementImpactController(GraduationRequirementImpactService service) {
        this.service = service;
    }

    @GetMapping("/{requirementEntityId}/impact")
    @Operation(summary = "졸업요건 변경 영향 조회", description = "[관리자 인증 O] 해당 학과·입학연도·전공 유형과 일치하는 미삭제 학생 수입니다. 학적 상태와 관계없이 집계하며 조회 이후 가입·전공 변경에 따라 달라질 수 있습니다. 개인정보 목록을 반환하지 않습니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 영향 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"ADMIN_ACCESS_DENIED\",\"message\":\"관리자 권한이 필요합니다.\"}"))),
        @ApiResponse(responseCode = "404", description = "졸업요건 없음", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(value = "{\"code\":\"GRADUATION_REQUIREMENT_NOT_FOUND\",\"message\":\"학생의 입학연도와 전공에 맞는 졸업요건을 찾을 수 없습니다.\"}")))
    })
    public GraduationRequirementImpactResponse getImpact(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID requirementEntityId
    ) {
        return service.getImpact(UUID.fromString(jwt.getSubject()), requirementEntityId);
    }
}
