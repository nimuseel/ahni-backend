package com.ahni.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CurriculumPublicationRequest(@NotNull Boolean published, @NotNull @PositiveOrZero Long version) { }
