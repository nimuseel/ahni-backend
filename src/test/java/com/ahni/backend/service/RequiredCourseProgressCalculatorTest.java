package com.ahni.backend.service;

import com.ahni.backend.domain.*;
import com.ahni.backend.entity.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RequiredCourseProgressCalculatorTest {
    private final Department department = new Department("소프트웨어융합공학과");
    private final Course course = new Course(department, "CSE101", "프로그래밍", new BigDecimal("3.0"), CourseCategory.MAJOR);
    private final Student student = new Student(java.util.UUID.randomUUID(), "student@inha.edu", 2024, EnrollmentStatus.ENROLLED, null);
    private final GraduationRequirement policy = new GraduationRequirement(department, 2024, MajorType.PRIMARY, new BigDecimal("130.0"), new BigDecimal("60.0"), new BigDecimal("30.0"), "학과 기준", null);
    private final RequiredCourse assignment = new RequiredCourse(policy, course, RequiredCourseCategory.MAJOR_REQUIRED);

    @Test
    void passing_P_and_RPL_complete_the_same_catalog_course() {
        for (GradeCode code : List.of(GradeCode.A_PLUS, GradeCode.D_ZERO, GradeCode.P)) {
            assertThat(RequiredCourseProgressCalculator.calculate(List.of(assignment), List.of(grade(code, null))).getFirst().completed()).isTrue();
        }
        var rpl = new StudentGrade(student, course, 2025, AcademicTerm.FIRST, null, new BigDecimal("3.0"), true, null);
        assertThat(RequiredCourseProgressCalculator.calculate(List.of(assignment), List.of(rpl)).getFirst().completed()).isTrue();
    }

    @Test
    void failed_missing_and_replaced_passing_attempts_do_not_complete_a_course() {
        var original = grade(GradeCode.A_PLUS, null);
        var failedRetake = grade(GradeCode.F, original);
        for (var grades : List.of(List.<StudentGrade>of(), List.of(grade(GradeCode.NP, null)), List.of(original, failedRetake))) {
            assertThat(RequiredCourseProgressCalculator.calculate(List.of(assignment), grades).getFirst().completed()).isFalse();
        }
    }

    @Test
    void unrelated_courses_and_duplicate_attempts_do_not_inflate_completion() {
        var unrelated = new Course(department, "CSE102", "다른 과목", new BigDecimal("3.0"), CourseCategory.MAJOR);
        var otherGrade = new StudentGrade(student, unrelated, 2025, AcademicTerm.FIRST, GradeCode.A_PLUS, new BigDecimal("3.0"), false, null);
        assertThat(RequiredCourseProgressCalculator.calculate(List.of(assignment), List.of(otherGrade)).getFirst().completed()).isFalse();
        var result = RequiredCourseProgressCalculator.calculate(List.of(assignment), List.of(grade(GradeCode.P, null), grade(GradeCode.A_PLUS, null)));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().entityId()).isEqualTo(assignment.getEntityId());
        assertThat(result.getFirst().completed()).isTrue();
    }

    private StudentGrade grade(GradeCode code, StudentGrade replaced) {
        return new StudentGrade(student, course, replaced == null ? 2024 : 2025, AcademicTerm.SECOND, code, new BigDecimal("3.0"), false, replaced);
    }
}
