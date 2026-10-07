package com.ahni.backend.dto;

import com.ahni.backend.domain.CourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CourseManagementRequest(
    @Schema(description = "관리 소속 학과(선택). 전공 인정 학과는 연도별 교과과정 연결로 결정합니다.", nullable = true) UUID departmentEntityId,
    @NotBlank @Size(max = 30) @Schema(example = "CSE101") String code,
    @NotBlank @Size(max = 200) @Schema(example = "프로그래밍 기초") String name,
    @NotNull @DecimalMin("0.0") @DecimalMax("30.0") @Digits(integer = 2, fraction = 1) @Schema(example = "3.0") BigDecimal credit,
    @NotNull @Schema(example = "MAJOR") CourseCategory category
) { }
