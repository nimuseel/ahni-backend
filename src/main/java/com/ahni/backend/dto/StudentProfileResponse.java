package com.ahni.backend.dto;

import com.ahni.backend.domain.AccountStatus;
import com.ahni.backend.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentProfileResponse(
    @Schema(description = "학생 외부 식별자", example = "00000000-0000-0000-0000-000000000020")
    UUID studentEntityId,

    @Schema(description = "인증된 학교 이메일", example = "student@inha.edu")
    String email,

    @Schema(description = "닉네임", example = "인하")
    String nickname,

    DepartmentResponse primaryDepartment,

    @Schema(description = "입학연도", example = "2024")
    Integer admissionYear,

    @Schema(description = "재학 상태", example = "ENROLLED")
    EnrollmentStatus enrollmentStatus,

    @Schema(description = "계정 상태", example = "ACTIVE")
    AccountStatus accountStatus
) {
}
