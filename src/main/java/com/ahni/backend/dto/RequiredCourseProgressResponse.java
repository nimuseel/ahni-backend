package com.ahni.backend.dto;
import com.ahni.backend.domain.RequiredCourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
public record RequiredCourseProgressResponse(
    @Schema(description = "필수과목 배정 식별자") UUID entityId,
    @Schema(description = "필수과목 분류") RequiredCourseCategory category,
    @Schema(description = "필수과목 정보") CourseResponse course,
    @Schema(description = "대체되지 않은 이수 성적 또는 P/RPL이 존재하는지", example = "true") boolean completed
) { }
