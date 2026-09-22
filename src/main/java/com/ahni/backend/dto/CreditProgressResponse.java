package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CreditProgressResponse(
    @Schema(description = "요구 학점", example = "130.0")
    BigDecimal required,
    @Schema(description = "이수 학점", example = "42.0")
    BigDecimal completed,
    @Schema(description = "남은 학점", example = "88.0")
    BigDecimal remaining,
    @Schema(description = "학점 기준 충족 여부", example = "false")
    boolean met
) { }
