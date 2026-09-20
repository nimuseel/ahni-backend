package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record GradeSummaryResponse(
    @Schema(description = "전체 GPA", example = "3.83")
    BigDecimal gpa,
    @Schema(description = "전체 이수학점", example = "42.0")
    BigDecimal completedCredits,
    @Schema(description = "GPA 계산에 포함된 학점", example = "36.0")
    BigDecimal gpaCredits,
    @Schema(description = "과목 분류별 GPA와 학점 요약")
    List<GradeCategorySummaryResponse> categories
) { }
