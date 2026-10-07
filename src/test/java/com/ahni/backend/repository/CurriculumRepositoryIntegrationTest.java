package com.ahni.backend.repository;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.CurriculumDivision;
import com.ahni.backend.domain.RecommendedTerm;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Curriculum;
import com.ahni.backend.entity.CurriculumCourse;
import com.ahni.backend.entity.Department;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Tag("integration")
class CurriculumRepositoryIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll static void stopPostgres() { POSTGRES.stop(); }
    @Autowired CurriculumRepository curricula;
    @Autowired CurriculumCourseRepository links;
    @Autowired DepartmentRepository departments;
    @Autowired CourseRepository courses;
    @Autowired EntityManager entityManager;

    @Test
    void reloadsAnnualLinksWithoutMergingSameNamedCourses() {
        var department = departments.saveAndFlush(new Department("교과과정검증학과"));
        var first = courses.saveAndFlush(new Course(department, "PLAN101", "같은 과목명", new BigDecimal("3.0"), CourseCategory.MAJOR));
        var second = courses.saveAndFlush(new Course(department, "PLAN201", "같은 과목명", new BigDecimal("3.0"), CourseCategory.MAJOR));
        var curriculum = curricula.saveAndFlush(new Curriculum(department, 2024, "공식 자료", "https://example.edu/curriculum"));
        links.saveAllAndFlush(List.of(
            new CurriculumCourse(curriculum, second, CurriculumDivision.MAJOR_ELECTIVE, null, null, null, null, null, null),
            new CurriculumCourse(curriculum, first, CurriculumDivision.MAJOR_REQUIRED, 1, RecommendedTerm.FIRST, "GED2", "영역", "전공영역", "비고")
        ));
        entityManager.clear();

        var found = curricula.findByEntityId(curriculum.getEntityId()).orElseThrow();
        var foundLinks = links.findAllByCurriculumOrderByCourseCodeAsc(found);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.isPublished()).isFalse();
        assertThat(foundLinks).extracting(link -> link.getCourse().getEntityId()).containsExactly(first.getEntityId(), second.getEntityId());
        assertThat(foundLinks.getFirst().getRecommendedTerm()).isEqualTo(RecommendedTerm.FIRST);
        assertThat(foundLinks.getFirst().getAreaCode()).isEqualTo("GED2");
        assertThat(foundLinks.getFirst().getNote()).isEqualTo("비고");
    }

    @Test
    void recognitionQueryLoadsSharedMajorsAndExcludesDraftsOtherYearsAndDeletedDepartments() {
        var owner = departments.saveAndFlush(new Department("관리학과"));
        var recognized = departments.saveAndFlush(new Department("인정학과"));
        var deleted = departments.saveAndFlush(new Department("삭제학과"));
        entityManager.createNativeQuery("update department set deleted_at = current_timestamp where id = :id")
            .setParameter("id", deleted.getId()).executeUpdate();
        var shared = courses.saveAndFlush(new Course(null, "SHARED101", "공통기초", new BigDecimal("3.0"), CourseCategory.MAJOR));
        var published = new Curriculum(recognized, 2025, "공식 자료", null);
        published.setPublished(true);
        var draft = new Curriculum(recognized, 2026, "공식 초안", null);
        var wrongYear = new Curriculum(recognized, 2024, "공식 자료", null);
        wrongYear.setPublished(true);
        var wrongDepartment = new Curriculum(owner, 2025, "다른 학과", null);
        wrongDepartment.setPublished(true);
        var deletedCurriculum = new Curriculum(deleted, 2025, "삭제 학과", null);
        deletedCurriculum.setPublished(true);
        for (var curriculum : List.of(published, draft, wrongYear, wrongDepartment, deletedCurriculum)) {
            curricula.saveAndFlush(curriculum);
            links.saveAndFlush(new CurriculumCourse(curriculum, shared, CurriculumDivision.MAJOR_FOUNDATION,
                null, null, null, null, null, null));
        }
        entityManager.clear();
        var result = links.findPublishedRecognitionLinks(
            List.of(recognized.getEntityId(), deleted.getEntityId()), List.of(2025, 2026));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCurriculum().getEntityId()).isEqualTo(published.getEntityId());
        assertThat(result.getFirst().getCourse().getEntityId()).isEqualTo(shared.getEntityId());
        assertThat(result.getFirst().getCourse().getDepartment()).isNull();
    }

    @Test
    void staleEditCannotOverwriteAChangedCurriculum() {
        var department = departments.saveAndFlush(new Department("편집충돌검증학과"));
        var stale = curricula.saveAndFlush(new Curriculum(department, 2024, "최초 자료", null));
        entityManager.clear();
        var current = curricula.findByEntityId(stale.getEntityId()).orElseThrow();
        current.updateSource("수정 자료", null);
        curricula.saveAndFlush(current);
        entityManager.clear();

        stale.updateSource("오래된 화면의 자료", null);
        assertThatThrownBy(() -> curricula.saveAndFlush(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
