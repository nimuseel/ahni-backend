package com.ahni.backend.dto;

import com.ahni.backend.domain.CourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record AdminCourseResponse(
    UUID entityId,
    @Schema(example = "CSE101") String code,
    @Schema(example = "프로그래밍 기초") String name,
    @Schema(example = "3.0") BigDecimal credit,
    CourseCategory category,
    @Schema(nullable = true) DepartmentResponse department,
    @Schema(description = "학생 과목 목록에 노출되는 활성 상태", example = "true") boolean active
) { }
