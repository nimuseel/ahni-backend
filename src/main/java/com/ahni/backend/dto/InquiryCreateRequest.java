package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
    @NotBlank
    @Size(max = 200)
    @Schema(description = "문의 제목", example = "성적 등록 문의")
    String title,

    @NotBlank
    @Size(max = 4000)
    @Schema(description = "문의 내용", example = "2025년 과목이 성적 등록 화면에 보이지 않습니다.")
    String content
) { }
