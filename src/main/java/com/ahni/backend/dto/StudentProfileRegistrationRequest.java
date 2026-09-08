package com.ahni.backend.dto;

import com.ahni.backend.domain.EnrollmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StudentProfileRegistrationRequest(
    @NotNull
    UUID primaryDepartmentEntityId,

    @NotNull
    @Min(2000)
    Integer admissionYear,

    @NotNull
    EnrollmentStatus enrollmentStatus,

    @Size(max = 100)
    String nickname
) {
}
