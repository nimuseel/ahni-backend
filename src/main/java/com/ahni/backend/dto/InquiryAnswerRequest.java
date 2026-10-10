package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryAnswerRequest(
    @NotBlank
    @Size(max = 4000)
    @Schema(description = "관리자 답변", example = "확인 후 성적 등록 과목 목록을 갱신했습니다.")
    String answer
) { }
