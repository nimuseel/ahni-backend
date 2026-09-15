package com.ahni.backend.entity;

import com.ahni.backend.domain.CourseCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTest {
    private final Department department = new Department("소프트웨어융합공학과");

    @Test
    void 과목_코드와_이름을_정규화한다() {
        Course course = new Course(
            department,
            " cse101 ",
            " 프로그래밍 기초 ",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );

        assertThat(course.getCode()).isEqualTo("CSE101");
        assertThat(course.getName()).isEqualTo("프로그래밍 기초");
    }

    @Test
    void 과목_코드는_필수다() {
        assertThatThrownBy(() -> createCourse(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createCourse(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createCourse("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과목_코드는_30자를_초과할_수_없다() {
        assertThatThrownBy(() -> createCourse("C".repeat(31)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과목명은_필수다() {
        assertThatThrownBy(() -> createCourseWithName(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createCourseWithName(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createCourseWithName("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과목명은_200자를_초과할_수_없다() {
        assertThatThrownBy(() -> createCourseWithName("가".repeat(201)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학점은_필수다() {
        assertThatThrownBy(() -> createCourseWithCredit(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학점은_0부터_30_사이여야_한다() {
        assertThatThrownBy(() -> createCourseWithCredit(new BigDecimal("-0.1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createCourseWithCredit(new BigDecimal("30.1")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(createCourseWithCredit(BigDecimal.ZERO).getCredit())
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(createCourseWithCredit(new BigDecimal("30.0")).getCredit())
            .isEqualByComparingTo(new BigDecimal("30.0"));
    }

    @Test
    void 학점은_소수점_한_자리까지만_허용한다() {
        assertThatThrownBy(() -> createCourseWithCredit(new BigDecimal("3.25")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(createCourseWithCredit(new BigDecimal("3.00")).getCredit())
            .isEqualByComparingTo(new BigDecimal("3.0"));
    }

    @Test
    void 과목_분류는_필수다() {
        assertThatThrownBy(() -> new Course(
            department,
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 전공_과목은_학과가_필수다() {
        assertThatThrownBy(() -> new Course(
            null,
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 교양_과목은_학과_없이_생성할_수_있다() {
        Course course = new Course(
            null,
            "GE101",
            "대학 글쓰기",
            new BigDecimal("2.0"),
            CourseCategory.GENERAL_EDUCATION
        );

        assertThat(course.getDepartment()).isNull();
    }

    private Course createCourse(String code) {
        return new Course(
            department,
            code,
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }

    private Course createCourseWithName(String name) {
        return new Course(
            department,
            "CSE101",
            name,
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }

    private Course createCourseWithCredit(BigDecimal credit) {
        return new Course(
            department,
            "CSE101",
            "프로그래밍 기초",
            credit,
            CourseCategory.MAJOR
        );
    }
}
