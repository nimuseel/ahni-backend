package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record GraduationRequirementImpactResponse(
    UUID requirementEntityId,
    Integer admissionYear,
    @Schema(allowableValues = {"PRIMARY", "DOUBLE_MAJOR", "MINOR"}) String majorType,
    DepartmentResponse department,
    @Schema(description = "조회 시점에 학과·입학연도·전공 유형이 일치하는 미삭제 학생 수. 재학·휴학·졸업 등 학적에 관계없이 포함", example = "7") long affectedStudentCount
) { }
