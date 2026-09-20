package com.ahni.backend.dto;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.GradeCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeUpdateRequest(
    @NotNull
    @Min(2000)
    @Schema(description = "수강연도", example = "2025", minimum = "2000")
    Integer academicYear,

    @NotNull
    @Schema(description = "수강학기", example = "SECOND")
    AcademicTerm term,

    @Schema(description = "성적 등급. RPL 성적은 null입니다.", example = "A_PLUS", nullable = true)
    GradeCode gradeCode,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("30.0")
    @Digits(integer = 2, fraction = 1)
    @Schema(description = "이수 학점", example = "3.0", minimum = "0.1", maximum = "30.0")
    BigDecimal credit,

    @Schema(description = "선행학습 인정 여부", defaultValue = "false")
    boolean rpl,

    @Schema(
        description = "재수강으로 대체하는 이전 성적 식별자",
        example = "00000000-0000-0000-0000-000000000200",
        nullable = true
    )
    UUID replacedGradeEntityId
) { }
