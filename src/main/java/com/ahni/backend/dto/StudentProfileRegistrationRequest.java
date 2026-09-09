package com.ahni.backend.dto;

import com.ahni.backend.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StudentProfileRegistrationRequest(
    @NotNull
    @Schema(description = "주전공 학과 식별자", example = "00000000-0000-0000-0000-000000000001")
    UUID primaryDepartmentEntityId,

    @NotNull
    @Min(2000)
    @Schema(description = "입학연도", example = "2024", minimum = "2000")
    Integer admissionYear,

    @NotNull
    @Schema(description = "가입 시 재학 상태", example = "ENROLLED", allowableValues = {"ENROLLED", "LEAVE"})
    EnrollmentStatus enrollmentStatus,

    @Size(max = 100)
    @Schema(description = "닉네임", example = "인하", maxLength = 100)
    String nickname
) {
}
