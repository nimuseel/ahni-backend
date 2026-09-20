package com.ahni.backend.dto;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.GradeCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
    @Schema(description = "성적 이력 식별자", example = "00000000-0000-0000-0000-000000000201")
    UUID entityId,
    GradeCourseResponse course,
    @Schema(description = "수강연도", example = "2025")
    int academicYear,
    @Schema(description = "수강학기", example = "SECOND")
    AcademicTerm term,
    @Schema(description = "성적 등급. RPL 성적은 null입니다.", example = "A_PLUS", nullable = true)
    GradeCode gradeCode,
    @Schema(description = "평점. P/NP와 RPL 성적은 null입니다.", example = "4.50", nullable = true)
    BigDecimal gradePoint,
    @Schema(description = "이수 학점 스냅샷", example = "3.0")
    BigDecimal credit,
    @Schema(description = "선행학습 인정 여부")
    boolean rpl,
    @Schema(
        description = "재수강으로 대체한 이전 성적 식별자",
        example = "00000000-0000-0000-0000-000000000200",
        nullable = true
    )
    UUID replacedGradeEntityId,
    @Schema(description = "등록 시각")
    Instant createdAt,
    @Schema(description = "수정 시각")
    Instant updatedAt
) { }
