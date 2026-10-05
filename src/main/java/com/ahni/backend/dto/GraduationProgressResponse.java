package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;
import java.util.List;

public record GraduationProgressResponse(
    @Schema(
        description = "졸업요건 식별자",
        example = "00000000-0000-0000-0000-000000000401"
    )
    UUID requirementEntityId,
    @Schema(description = "적용 입학연도", example = "2024")
    Integer admissionYear,
    @Schema(
        description = "적용 전공 유형",
        example = "PRIMARY",
        allowableValues = {"PRIMARY", "DOUBLE_MAJOR", "MINOR"}
    )
    String majorType,
    @Schema(description = "적용 학과")
    DepartmentResponse department,
    @Schema(description = "학점 충족도")
    GraduationCreditProgressResponse credits,
    @Schema(description = "필수과목별 이수 여부") List<RequiredCourseProgressResponse> requiredCourses,
    @Schema(description = "학점 및 필수과목 기준 충족 여부. 학교의 최종 졸업 판정이 아님", example = "false") boolean requirementsMet
) { }
