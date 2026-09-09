package com.ahni.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiErrorResponse(
    @Schema(description = "안정적인 애플리케이션 오류 코드", example = "INVALID_REQUEST")
    String code,

    @Schema(description = "사용자에게 표시할 수 있는 오류 메시지", example = "요청값이 올바르지 않습니다.")
    String message
) {
}
