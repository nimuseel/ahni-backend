package com.ahni.backend.dto;

import java.util.UUID;

public record DepartmentResponse(
    UUID entityId,
    String name
) {
}
