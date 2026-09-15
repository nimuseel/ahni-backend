package com.ahni.backend.repository;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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
class CourseRepositoryIntegrationTest {
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
        registry.add(
            "spring.datasource.driver-class-name",
            POSTGRES::getDriverClassName
        );
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearCatalog() {
        courseRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    @Test
    void 과목을_저장하고_코드순으로_조회한다() {
        Department department = saveDepartment("소프트웨어융합공학과");
        courseRepository.saveAllAndFlush(List.of(
            majorCourse(department, "CSE201", "자료구조"),
            majorCourse(department, "CSE101", "프로그래밍 기초")
        ));

        entityManager.clear();

        List<Course> courses = courseRepository.findAllByActiveTrueOrderByCodeAsc();

        assertThat(courses)
            .extracting(Course::getCode)
            .containsExactly("CSE101", "CSE201");
        assertThat(courses.getFirst().getEntityId()).isNotNull();
        assertThat(courses.getFirst().getCreatedAt()).isNotNull();
        assertThat(courses.getFirst().getUpdatedAt()).isNotNull();
    }

    @Test
    void 학과와_분류로_활성_과목을_조회한다() {
        Department software = saveDepartment("소프트웨어융합공학과");
        Department finance = saveDepartment("금융투자학과");
        courseRepository.saveAllAndFlush(List.of(
            majorCourse(software, "CSE101", "프로그래밍 기초"),
            majorCourse(finance, "FIN101", "금융학 개론"),
            generalCourse("GE101", "대학 글쓰기")
        ));

        List<Course> courses = courseRepository
            .findAllByActiveTrueAndDepartmentAndCategoryOrderByCodeAsc(
                software,
                CourseCategory.MAJOR
            );

        assertThat(courses)
            .extracting(Course::getCode)
            .containsExactly("CSE101");
    }

    @Test
    void 학과만으로_활성_과목을_조회한다() {
        Department software = saveDepartment("소프트웨어융합공학과");
        Department finance = saveDepartment("금융투자학과");
        courseRepository.saveAllAndFlush(List.of(
            majorCourse(software, "CSE201", "자료구조"),
            majorCourse(software, "CSE101", "프로그래밍 기초"),
            majorCourse(finance, "FIN101", "금융학 개론")
        ));

        List<Course> courses = courseRepository
            .findAllByActiveTrueAndDepartmentOrderByCodeAsc(software);

        assertThat(courses)
            .extracting(Course::getCode)
            .containsExactly("CSE101", "CSE201");
    }

    @Test
    void 분류만으로_활성_과목을_조회한다() {
        Department department = saveDepartment("소프트웨어융합공학과");
        courseRepository.saveAllAndFlush(List.of(
            majorCourse(department, "CSE101", "프로그래밍 기초"),
            generalCourse("GE101", "대학 글쓰기")
        ));

        List<Course> courses = courseRepository
            .findAllByActiveTrueAndCategoryOrderByCodeAsc(CourseCategory.GENERAL_EDUCATION);

        assertThat(courses)
            .extracting(Course::getCode)
            .containsExactly("GE101");
    }

    @Test
    void 비활성_과목은_조회하지_않는다() {
        Department department = saveDepartment("소프트웨어융합공학과");
        Course active = courseRepository.saveAndFlush(
            majorCourse(department, "CSE101", "프로그래밍 기초")
        );
        Course inactive = courseRepository.saveAndFlush(
            majorCourse(department, "CSE201", "자료구조")
        );
        jdbcTemplate.update(
            "UPDATE public.course SET is_active = false WHERE id = ?",
            inactive.getId()
        );
        entityManager.clear();

        List<Course> courses = courseRepository.findAllByActiveTrueOrderByCodeAsc();

        assertThat(courses)
            .extracting(Course::getEntityId)
            .containsExactly(active.getEntityId());
    }

    @Test
    void 교양_과목은_학과_없이_저장할_수_있다() {
        Course saved = courseRepository.saveAndFlush(generalCourse("GE101", "대학 글쓰기"));

        entityManager.clear();

        Course found = courseRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getDepartment()).isNull();
    }

    @Test
    void 동일한_과목_코드는_저장할_수_없다() {
        Department department = saveDepartment("소프트웨어융합공학과");
        courseRepository.saveAndFlush(
            majorCourse(department, "CSE101", "프로그래밍 기초")
        );

        assertThatThrownBy(() -> courseRepository.saveAndFlush(
            majorCourse(department, "CSE101", "컴퓨터 개론")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 데이터베이스는_소문자_과목_코드를_거부한다() {
        assertThatThrownBy(() -> insertRawCourse("cse101", "3.0", "GENERAL_EDUCATION"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 데이터베이스는_범위를_벗어난_학점을_거부한다() {
        assertThatThrownBy(() -> insertRawCourse("GE101", "30.1", "GENERAL_EDUCATION"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 데이터베이스는_유효하지_않은_분류를_거부한다() {
        assertThatThrownBy(() -> insertRawCourse("GE101", "3.0", "UNKNOWN"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 데이터베이스는_학과가_없는_전공_과목을_거부한다() {
        assertThatThrownBy(() -> insertRawCourse("CSE101", "3.0", "MAJOR"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Department saveDepartment(String name) {
        return departmentRepository.saveAndFlush(new Department(name));
    }

    private Course majorCourse(Department department, String code, String name) {
        return new Course(
            department,
            code,
            name,
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }

    private Course generalCourse(String code, String name) {
        return new Course(
            null,
            code,
            name,
            new BigDecimal("2.0"),
            CourseCategory.GENERAL_EDUCATION
        );
    }

    private void insertRawCourse(String code, String credit, String category) {
        jdbcTemplate.update(
            """
                INSERT INTO public.course (code, name, credit, category)
                VALUES (?, '테스트 과목', ?, ?)
                """,
            code,
            new BigDecimal(credit),
            category
        );
    }
}
