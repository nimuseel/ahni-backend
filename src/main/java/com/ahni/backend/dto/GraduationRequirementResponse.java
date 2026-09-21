package com.ahni.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GraduationRequirementResponse(
    UUID entityId,
    Integer admissionYear,
    String majorType,
    DepartmentResponse department,
    BigDecimal minTotalCredit,
    BigDecimal minMajorCredit,
    BigDecimal minDoubleMajorCredit,
    List<RequiredCourseResponse> requiredCourses
) { }
