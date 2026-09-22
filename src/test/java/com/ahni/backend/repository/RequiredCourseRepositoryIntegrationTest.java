package com.ahni.backend.repository;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.RequiredCourseCategory;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.RequiredCourse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Tag("integration")
class RequiredCourseRepositoryIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:18");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Autowired
    private RequiredCourseRepository requiredCourseRepository;

    @Autowired
    private GraduationRequirementRepository graduationRequirementRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearRequiredCourses() {
        requiredCourseRepository.deleteAllInBatch();
        graduationRequirementRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    @Test
    void 졸업요건의_활성_필수과목을_과목코드순으로_조회한다() {
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        GraduationRequirement requirement = graduationRequirementRepository.saveAndFlush(
            graduationRequirement(department)
        );
        Course laterCourse = courseRepository.saveAndFlush(
            course(department, "CSE201", "자료구조")
        );
        Course earlierCourse = courseRepository.saveAndFlush(
            course(department, "CSE101", "프로그래밍 기초")
        );
        requiredCourseRepository.saveAllAndFlush(List.of(
            new RequiredCourse(
                requirement,
                laterCourse,
                RequiredCourseCategory.MAJOR_REQUIRED
            ),
            new RequiredCourse(
                requirement,
                earlierCourse,
                RequiredCourseCategory.MAJOR_FOUNDATION
            )
        ));

        entityManager.clear();

        List<RequiredCourse> requiredCourses = requiredCourseRepository
            .findAllActiveByGraduationRequirement(requirement);

        assertThat(requiredCourses)
            .extracting(requiredCourse -> requiredCourse.getCourse().getCode())
            .containsExactly("CSE101", "CSE201");
        assertThat(requiredCourses)
            .extracting(RequiredCourse::getCategory)
            .containsExactly(
                RequiredCourseCategory.MAJOR_FOUNDATION,
                RequiredCourseCategory.MAJOR_REQUIRED
            );
    }

    @Test
    void 소프트_삭제한_필수과목은_활성_조회에서_제외한다() {
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        GraduationRequirement requirement = graduationRequirementRepository.saveAndFlush(
            graduationRequirement(department)
        );
        Course course = courseRepository.saveAndFlush(
            course(department, "CSE101", "프로그래밍 기초")
        );
        RequiredCourse requiredCourse = requiredCourseRepository.saveAndFlush(
            new RequiredCourse(
                requirement,
                course,
                RequiredCourseCategory.MAJOR_FOUNDATION
            )
        );

        requiredCourse.softDelete();
        requiredCourseRepository.saveAndFlush(requiredCourse);
        entityManager.clear();

        assertThat(requiredCourseRepository.findAllActiveByGraduationRequirement(requirement))
            .isEmpty();
    }

    @Test
    void 같은_졸업요건에_같은_과목을_중복_등록할_수_없다() {
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        GraduationRequirement requirement = graduationRequirementRepository.saveAndFlush(
            graduationRequirement(department)
        );
        Course course = courseRepository.saveAndFlush(
            course(department, "CSE101", "프로그래밍 기초")
        );
        requiredCourseRepository.saveAndFlush(
            new RequiredCourse(
                requirement,
                course,
                RequiredCourseCategory.MAJOR_FOUNDATION
            )
        );

        assertThatThrownBy(() -> requiredCourseRepository.saveAndFlush(
            new RequiredCourse(
                requirement,
                course,
                RequiredCourseCategory.MAJOR_REQUIRED
            )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private GraduationRequirement graduationRequirement(Department department) {
        return new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            new BigDecimal("30.0")
        );
    }

    private Course course(Department department, String code, String name) {
        return new Course(
            department,
            code,
            name,
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }
}
