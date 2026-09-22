package com.ahni.backend.service;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.dto.GraduationCreditProgressResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GraduationProgressCalculatorTest {
    private Student student;
    private Department department;

    @BeforeEach
    void setUp() {
        student = new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
        department = new Department("소프트웨어융합공학과");
    }

    @Test
    void 전체_전공_교양_학점의_충족도를_계산한다() {
        GraduationRequirement requirement = requirement("6.0", "3.0", "2.0");
        List<StudentGrade> grades = List.of(
            grade(majorCourse(department, "CSE101"), GradeCode.A_PLUS, "3.0"),
            grade(generalCourse("GED101"), GradeCode.P, "2.0"),
            rpl(electiveCourse("ELE101"), "2.0")
        );

        GraduationCreditProgressResponse result = GraduationProgressCalculator
            .calculate(requirement, grades);

        assertThat(result.total().required()).isEqualByComparingTo("6.0");
        assertThat(result.total().completed()).isEqualByComparingTo("7.0");
        assertThat(result.total().remaining()).isEqualByComparingTo("0.0");
        assertThat(result.total().met()).isTrue();
        assertThat(result.department().completed()).isEqualByComparingTo("3.0");
        assertThat(result.department().met()).isTrue();
        assertThat(result.general().completed()).isEqualByComparingTo("2.0");
        assertThat(result.general().met()).isTrue();
    }

    @Test
    void 대체된_성적과_미이수_성적은_제외하고_학과를_구분한다() {
        GraduationRequirement requirement = requirement("10.0", "6.0", "2.0");
        Course retakenCourse = majorCourse(department, "CSE201");
        StudentGrade original = grade(
            retakenCourse,
            2024,
            AcademicTerm.FIRST,
            GradeCode.A_PLUS,
            null
        );
        StudentGrade retake = grade(
            retakenCourse,
            2025,
            AcademicTerm.FIRST,
            GradeCode.F,
            original
        );
        Department otherDepartment = new Department("금융투자학과");

        GraduationCreditProgressResponse result = GraduationProgressCalculator
            .calculate(requirement, List.of(
                original,
                retake,
                grade(
                    majorCourse(otherDepartment, "FIN101"),
                    GradeCode.A_ZERO,
                    "3.0"
                ),
                grade(generalCourse("GED201"), GradeCode.NP, "2.0")
            ));

        assertThat(result.total().completed()).isEqualByComparingTo("3.0");
        assertThat(result.total().remaining()).isEqualByComparingTo("7.0");
        assertThat(result.department().completed()).isEqualByComparingTo("0.0");
        assertThat(result.department().remaining()).isEqualByComparingTo("6.0");
        assertThat(result.general().completed()).isEqualByComparingTo("0.0");
        assertThat(result.general().remaining()).isEqualByComparingTo("2.0");
        assertThat(result.total().met()).isFalse();
    }

    private GraduationRequirement requirement(
        String total,
        String departmentCredit,
        String general
    ) {
        return new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal(total),
            new BigDecimal(departmentCredit),
            new BigDecimal(general),
            "2024학년도 졸업요건",
            null
        );
    }

    private Course majorCourse(Department owner, String code) {
        return new Course(
            owner,
            code,
            code,
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }

    private Course generalCourse(String code) {
        return new Course(
            null,
            code,
            code,
            new BigDecimal("3.0"),
            CourseCategory.GENERAL_EDUCATION
        );
    }

    private Course electiveCourse(String code) {
        return new Course(
            null,
            code,
            code,
            new BigDecimal("3.0"),
            CourseCategory.ELECTIVE
        );
    }

    private StudentGrade grade(Course course, GradeCode gradeCode, String credit) {
        return grade(
            course,
            2025,
            AcademicTerm.SECOND,
            gradeCode,
            null,
            credit
        );
    }

    private StudentGrade grade(
        Course course,
        int academicYear,
        AcademicTerm term,
        GradeCode gradeCode,
        StudentGrade replacedGrade
    ) {
        return grade(
            course,
            academicYear,
            term,
            gradeCode,
            replacedGrade,
            "3.0"
        );
    }

    private StudentGrade grade(
        Course course,
        int academicYear,
        AcademicTerm term,
        GradeCode gradeCode,
        StudentGrade replacedGrade,
        String credit
    ) {
        return new StudentGrade(
            student,
            course,
            academicYear,
            term,
            gradeCode,
            new BigDecimal(credit),
            false,
            replacedGrade
        );
    }

    private StudentGrade rpl(Course course, String credit) {
        return new StudentGrade(
            student,
            course,
            2025,
            AcademicTerm.SECOND,
            null,
            new BigDecimal(credit),
            true,
            null
        );
    }
}
