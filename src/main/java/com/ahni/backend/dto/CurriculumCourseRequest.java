package com.ahni.backend.dto;

import com.ahni.backend.domain.CurriculumDivision;
import com.ahni.backend.domain.RecommendedTerm;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record CurriculumCourseRequest(
    @NotNull UUID courseEntityId, @NotNull CurriculumDivision division,
    @Min(1) @Max(6) Integer recommendedYear, RecommendedTerm recommendedTerm,
    @Size(max = 30) String areaCode, @Size(max = 100) String areaName,
    @Size(max = 100) String majorArea, @Size(max = 4000) String note
) { }
