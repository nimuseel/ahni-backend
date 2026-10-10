package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AdminInquiryStudentResponse(
    @Schema(description = "학생 식별자")
    UUID entityId,

    @Schema(description = "학생 이메일")
    String email,

    @Schema(description = "학생 닉네임", nullable = true)
    String nickname
) { }
