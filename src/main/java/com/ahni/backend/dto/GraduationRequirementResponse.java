package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GraduationRequirementResponse(
    @Schema(
        description = "졸업요건 식별자",
        example = "00000000-0000-0000-0000-000000000401"
    )
    UUID entityId,
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
    @Schema(description = "최소 총 이수학점", example = "130.0")
    BigDecimal minTotalCredit,
    @Schema(description = "최소 전공 이수학점", example = "60.0")
    BigDecimal minMajorCredit,
    @Schema(description = "최소 복수전공 이수학점", example = "0.0")
    BigDecimal minDoubleMajorCredit,
    @Schema(description = "활성 필수과목 목록")
    List<RequiredCourseResponse> requiredCourses
) { }
