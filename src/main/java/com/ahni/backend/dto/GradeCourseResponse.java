package com.ahni.backend.dto;

import com.ahni.backend.domain.CourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record GradeCourseResponse(
    @Schema(description = "과목 식별자", example = "00000000-0000-0000-0000-000000000101")
    UUID entityId,
    @Schema(description = "과목 코드", example = "CSE101")
    String code,
    @Schema(description = "과목명", example = "프로그래밍 기초")
    String name,
    @Schema(description = "과목 분류", example = "MAJOR")
    CourseCategory category,
    @Schema(description = "소속 학과. 공통 과목은 null입니다.", nullable = true)
    DepartmentResponse department
) { }
