package com.ahni.backend.dto;

import com.ahni.backend.domain.RequiredCourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequiredCourseAssignmentRequest(
    @NotNull
    @Schema(
        description = "필수과목으로 지정할 과목 식별자",
        example = "00000000-0000-0000-0000-000000000101"
    )
    UUID courseEntityId,
    @NotNull
    @Schema(description = "필수과목 분류", example = "MAJOR_FOUNDATION")
    RequiredCourseCategory category
) { }
