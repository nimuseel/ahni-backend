package com.ahni.backend.dto;

import com.ahni.backend.domain.CurriculumDivision;
import com.ahni.backend.domain.RecommendedTerm;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurriculumCourseResponse(UUID entityId, CourseResponse course, CurriculumDivision division,
    @Schema(nullable = true) Integer recommendedYear, @Schema(nullable = true) RecommendedTerm recommendedTerm, @Schema(nullable = true) String areaCode,
    @Schema(nullable = true) String areaName, @Schema(nullable = true) String majorArea, @Schema(nullable = true) String note) { }
