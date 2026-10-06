package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GradeSimulationRequest(
    @NotNull @Size(min = 1, max = 50)
    @Schema(description = "저장하지 않는 예상 성적 목록, 1~50건")
    List<@NotNull @Valid ExpectedGradeRequest> expectedGrades
) { }
