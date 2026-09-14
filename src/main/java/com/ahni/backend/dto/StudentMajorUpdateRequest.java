package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StudentMajorUpdateRequest(
    @NotNull
    @Schema(description = "주전공 학과 식별자")
    UUID primaryDepartmentEntityId,

    @Schema(description = "복수전공 학과 식별자", nullable = true)
    UUID doubleMajorDepartmentEntityId,

    @Schema(description = "부전공 학과 식별자", nullable = true)
    UUID minorDepartmentEntityId
) {
}
