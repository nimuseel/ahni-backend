package com.ahni.backend.dto;

import com.ahni.backend.domain.AccountStatus;
import com.ahni.backend.domain.EnrollmentStatus;

import java.util.UUID;

public record StudentProfileResponse(
    UUID studentEntityId,
    String email,
    String nickname,
    DepartmentResponse primaryDepartment,
    Integer admissionYear,
    EnrollmentStatus enrollmentStatus,
    AccountStatus accountStatus
) {
}
