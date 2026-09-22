package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GraduationCreditProgressResponse(
    @Schema(description = "전체 학점 충족도")
    CreditProgressResponse total,
    @Schema(description = "해당 학과 전공 학점 충족도")
    CreditProgressResponse department,
    @Schema(description = "교양 학점 충족도")
    CreditProgressResponse general
) { }
