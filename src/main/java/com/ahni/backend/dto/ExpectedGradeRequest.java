package com.ahni.backend.dto;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.GradeCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExpectedGradeRequest(
    @NotNull @Schema(description = "예상 과목 분류", example = "MAJOR")
    CourseCategory category,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("30.0")
    @Digits(integer = 2, fraction = 1)
    @Schema(description = "0 초과 30 이하, 소수점 한 자리 학점", example = "3.0")
    BigDecimal credit,
    @NotNull @Schema(description = "예상 등급. P/NP는 GPA에서 제외, F는 0점으로 포함", example = "A_PLUS")
    GradeCode gradeCode
) { }
