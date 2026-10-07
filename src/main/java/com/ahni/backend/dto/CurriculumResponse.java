package com.ahni.backend.dto;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurriculumResponse(UUID entityId, long version, DepartmentResponse department,
    int curriculumYear, String sourceTitle, @Schema(nullable = true) String sourceUrl, boolean published,
    List<CurriculumCourseResponse> courses) { }
