package com.ahni.backend.entity;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.RequiredCourseCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredCourseTest {
    private final Department department = new Department("소프트웨어융합공학과");
    private final GraduationRequirement requirement = new GraduationRequirement(
        department,
        2024,
        MajorType.PRIMARY,
        new BigDecimal("130.0"),
        new BigDecimal("60.0"),
        new BigDecimal("30.0"),
        "2024학년도 졸업요건",
        null
    );
    private final Course course = new Course(
        department,
        "CSE101",
        "프로그래밍 기초",
        new BigDecimal("3.0"),
        CourseCategory.MAJOR
    );

    @Test
    void 졸업요건에_필수과목을_분류해_등록한다() {
        RequiredCourse requiredCourse = new RequiredCourse(
            requirement,
            course,
            RequiredCourseCategory.MAJOR_FOUNDATION
        );

        assertThat(requiredCourse.getGraduationRequirement()).isSameAs(requirement);
        assertThat(requiredCourse.getCourse()).isSameAs(course);
        assertThat(requiredCourse.getCategory())
            .isEqualTo(RequiredCourseCategory.MAJOR_FOUNDATION);
        assertThat(requiredCourse.getDeletedAt()).isNull();
        assertThat(requiredCourse.isActive()).isTrue();
    }

    @Test
    void 필수과목을_소프트_삭제한다() {
        RequiredCourse requiredCourse = new RequiredCourse(
            requirement,
            course,
            RequiredCourseCategory.MAJOR_REQUIRED
        );

        requiredCourse.softDelete();

        assertThat(requiredCourse.getDeletedAt()).isNotNull();
        assertThat(requiredCourse.isActive()).isFalse();
    }

    @Test
    void 필수과목의_필수값을_검증한다() {
        assertThatThrownBy(() -> new RequiredCourse(
            null,
            course,
            RequiredCourseCategory.GENERAL_REQUIRED
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RequiredCourse(
            requirement,
            null,
            RequiredCourseCategory.GENERAL_REQUIRED
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RequiredCourse(
            requirement,
            course,
            null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
