package com.ahni.backend.dto;

import com.ahni.backend.domain.RequiredCourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record RequiredCourseResponse(
    @Schema(
        description = "필수과목 관계 식별자",
        example = "00000000-0000-0000-0000-000000000301"
    )
    UUID entityId,
    @Schema(description = "졸업요건 내 필수과목 분류", example = "MAJOR_FOUNDATION")
    RequiredCourseCategory category,
    @Schema(description = "과목 정보")
    CourseResponse course
) { }
