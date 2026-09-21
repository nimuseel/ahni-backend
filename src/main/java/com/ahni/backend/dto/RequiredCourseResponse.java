package com.ahni.backend.dto;

import com.ahni.backend.domain.RequiredCourseCategory;

import java.util.UUID;

public record RequiredCourseResponse(
    UUID entityId,
    RequiredCourseCategory category,
    CourseResponse course
) { }
