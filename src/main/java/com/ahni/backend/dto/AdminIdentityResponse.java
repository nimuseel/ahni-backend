package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AdminIdentityResponse(
    @Schema(description = "관리자 외부 식별자")
    UUID entityId,

    @Schema(description = "관리자 이름")
    String name,

    @Schema(description = "관리자 이메일")
    String email
) {
}
