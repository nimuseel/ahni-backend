package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GradeSimulationResponse(
    @Schema(description = "등록된 실제 성적의 요약") GradeSummaryResponse current,
    @Schema(description = "실제 성적에 예상 성적을 추가한 요약. 실제 성적을 변경하지 않음")
    GradeSummaryResponse projected
) { }
