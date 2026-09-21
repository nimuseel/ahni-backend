package com.ahni.backend.dto;

import com.ahni.backend.domain.CourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record GradeCategorySummaryResponse(
    @Schema(description = "과목 분류", example = "MAJOR")
    CourseCategory category,
    @Schema(description = "분류별 GPA", example = "4.02")
    BigDecimal gpa,
    @Schema(description = "분류별 이수학점", example = "24.0")
    BigDecimal completedCredits,
    @Schema(description = "분류별 GPA 계산 학점", example = "21.0")
    BigDecimal gpaCredits
) { }
