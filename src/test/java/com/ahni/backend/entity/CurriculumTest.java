package com.ahni.backend.entity;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.CurriculumDivision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurriculumTest {
    private final Department department = new Department("소프트웨어융합공학과");

    @Test
    void rejectsInvalidYearAndBlankSource() {
        assertThrows(IllegalArgumentException.class, () -> new Curriculum(department, 1999, "자료", null));
        assertThrows(IllegalArgumentException.class, () -> new Curriculum(department, 2024, "  ", null));
        assertThrows(IllegalArgumentException.class, () -> new Curriculum(department, 2024, "자료", "javascript:alert(1)"));
    }

    @Test
    void editingPublishedCurriculumReturnsToDraft() {
        var curriculum = new Curriculum(department, 2024, " 자료 ", null);
        curriculum.setPublished(true);
        curriculum.updateSource("새 자료", null);
        assertFalse(curriculum.isPublished());
        assertEquals("새 자료", curriculum.getSourceTitle());
    }

    @Test
    void optionalRecommendationIsAllowedButOutOfRangeYearIsRejected() {
        var curriculum = new Curriculum(department, 2024, "자료", null);
        var course = new Course(department, "ITC1201", "과목", new BigDecimal("3.0"), CourseCategory.MAJOR);
        var link = new CurriculumCourse(curriculum, course, CurriculumDivision.MAJOR_REQUIRED, null, null, null, null, null, "  ");
        assertNull(link.getRecommendedYear());
        assertNull(link.getNote());
        assertThrows(IllegalArgumentException.class, () -> new CurriculumCourse(curriculum, course,
            CurriculumDivision.MAJOR_REQUIRED, 0, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CurriculumCourse(curriculum, course,
            CurriculumDivision.MAJOR_REQUIRED, null, null, null, null, null, "x".repeat(4001)));
    }
}
