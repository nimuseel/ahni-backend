package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DepartmentResponse(
    @Schema(
        description = "학과 식별자",
        example = "00000000-0000-0000-0000-000000000001"
    )
    UUID entityId,
    @Schema(
        description = "학과명",
        example = "소프트웨어융합공학과"
    )
    String name
) {
}
