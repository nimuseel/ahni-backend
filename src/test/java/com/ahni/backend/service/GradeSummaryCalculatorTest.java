package com.ahni.backend.service;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.dto.GradeCategorySummaryResponse;
import com.ahni.backend.dto.GradeSummaryResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GradeSummaryCalculatorTest {
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
    void 과목별_학점을_가중치로_전체_GPA를_계산한다() {
        StudentGrade threeCreditAPlus = grade(
            course("CSE101", CourseCategory.MAJOR),
            GradeCode.A_PLUS,
            "3.0"
        );
        StudentGrade oneCreditBZero = grade(
            course("CSE102", CourseCategory.MAJOR),
            GradeCode.B_ZERO,
            "1.0"
        );

        GradeSummaryResponse result = GradeSummaryCalculator.calculate(
            List.of(threeCreditAPlus, oneCreditBZero)
        );

        assertThat(result.gpa()).isEqualByComparingTo("4.13");
        assertThat(result.completedCredits()).isEqualByComparingTo("4.0");
        assertThat(result.gpaCredits()).isEqualByComparingTo("4.0");
    }

    @Test
    void 성적_종류별로_이수학점과_GPA_반영_여부를_구분한다() {
        List<StudentGrade> grades = List.of(
            grade(course("CSE201", CourseCategory.MAJOR), GradeCode.A_ZERO, "3.0"),
            grade(course("CSE202", CourseCategory.MAJOR), GradeCode.F, "2.0"),
            grade(course("GED101", CourseCategory.GENERAL_EDUCATION), GradeCode.P, "1.0"),
            grade(course("GED102", CourseCategory.GENERAL_EDUCATION), GradeCode.NP, "1.5"),
            rpl(course("ELE101", CourseCategory.ELECTIVE), "2.5")
        );

        GradeSummaryResponse result = GradeSummaryCalculator.calculate(grades);

        assertThat(result.gpa()).isEqualByComparingTo("2.40");
        assertThat(result.completedCredits()).isEqualByComparingTo("6.5");
        assertThat(result.gpaCredits()).isEqualByComparingTo("5.0");
    }

    @Test
    void 성적이_없으면_모든_합계를_0으로_반환한다() {
        GradeSummaryResponse result = GradeSummaryCalculator.calculate(List.of());

        assertThat(result.gpa()).isEqualByComparingTo("0.00");
        assertThat(result.completedCredits()).isEqualByComparingTo("0.0");
        assertThat(result.gpaCredits()).isEqualByComparingTo("0.0");
    }

    @Test
    void 과목_분류별_요약과_빈_분류를_모두_반환한다() {
        GradeSummaryResponse result = GradeSummaryCalculator.calculate(List.of(
            grade(course("CSE301", CourseCategory.MAJOR), GradeCode.A_PLUS, "3.0"),
            grade(
                course("GED201", CourseCategory.GENERAL_EDUCATION),
                GradeCode.P,
                "2.0"
            )
        ));

        assertThat(result.categories())
            .extracting(GradeCategorySummaryResponse::category)
            .containsExactly(
                CourseCategory.MAJOR,
                CourseCategory.GENERAL_EDUCATION,
                CourseCategory.ELECTIVE
            );
        assertThat(result.categories().get(0).gpa())
            .isEqualByComparingTo("4.50");
        assertThat(result.categories().get(0).completedCredits())
            .isEqualByComparingTo("3.0");
        assertThat(result.categories().get(1).gpa())
            .isEqualByComparingTo("0.00");
        assertThat(result.categories().get(1).completedCredits())
            .isEqualByComparingTo("2.0");
        assertThat(result.categories().get(2).gpaCredits())
            .isEqualByComparingTo("0.0");
    }

    @Test
    void 여러_번_재수강한_경우_마지막_성적만_집계한다() {
        Course course = course("CSE401", CourseCategory.MAJOR);
        StudentGrade original = grade(
            course,
            2023,
            AcademicTerm.FIRST,
            GradeCode.C_PLUS,
            null
        );
        StudentGrade firstRetake = grade(
            course,
            2024,
            AcademicTerm.FIRST,
            GradeCode.B_PLUS,
            original
        );
        StudentGrade latestRetake = grade(
            course,
            2025,
            AcademicTerm.FIRST,
            GradeCode.A_PLUS,
            firstRetake
        );

        GradeSummaryResponse result = GradeSummaryCalculator.calculate(
            List.of(original, firstRetake, latestRetake)
        );

        assertThat(result.gpa()).isEqualByComparingTo("4.50");
        assertThat(result.completedCredits()).isEqualByComparingTo("3.0");
        assertThat(result.gpaCredits()).isEqualByComparingTo("3.0");
    }

    private Course course(String code, CourseCategory category) {
        return new Course(
            category == CourseCategory.MAJOR ? department : null,
            code,
            code,
            new BigDecimal("3.0"),
            category
        );
    }

    private StudentGrade grade(Course course, GradeCode gradeCode, String credit) {
        return new StudentGrade(
            student,
            course,
            2025,
            AcademicTerm.SECOND,
            gradeCode,
            new BigDecimal(credit),
            false,
            null
        );
    }

    private StudentGrade grade(
        Course course,
        int academicYear,
        AcademicTerm term,
        GradeCode gradeCode,
        StudentGrade replacedGrade
    ) {
        return new StudentGrade(
            student,
            course,
            academicYear,
            term,
            gradeCode,
            new BigDecimal("3.0"),
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
