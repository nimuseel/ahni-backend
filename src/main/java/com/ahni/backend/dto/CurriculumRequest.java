package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record CurriculumRequest(
    @NotNull UUID departmentEntityId,
    @NotNull @Min(2000) @Max(9999) Integer curriculumYear,
    @NotBlank @Size(max = 200) String sourceTitle,
    @Size(max = 2048) String sourceUrl,
    @PositiveOrZero @Schema(description = "생성 시 null, 수정 시 조회한 편집 버전") Long version,
    @NotNull @Size(max = 2000) List<@NotNull @Valid CurriculumCourseRequest> courses
) { }
