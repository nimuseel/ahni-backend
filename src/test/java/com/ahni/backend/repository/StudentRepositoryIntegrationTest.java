package com.ahni.backend.repository;

import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.entity.*;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Tag("integration")
class StudentRepositoryIntegrationTest {
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
    private StudentRepository studentRepository;

    @Autowired
    private StudentMajorRepository studentMajorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void removeSeededDepartments() {
        departmentRepository.deleteAllInBatch();
    }

    @Test
    void authUserId로_학생을_조회할_수_있다() {
        UUID test = UUID.randomUUID();

        studentRepository.saveAllAndFlush(List.of(
            new Student(
                UUID.randomUUID(),
                "other@inha.edu",
                2023,
                EnrollmentStatus.ENROLLED,
                "학생1"
            ),
            new Student(
                test,
                "target@inha.edu",
                2024,
                EnrollmentStatus.LEAVE,
                "조회 대상 학생"
            )
        ));

        entityManager.clear();

        Student found = studentRepository.findByAuthUserId(test).orElseThrow();

        assertThat(found.getAuthUserId()).isEqualTo(test);
        assertThat(found.getEmail()).isEqualTo("target@inha.edu");
        assertThat(found.getAdmissionYear()).isEqualTo((short) 2024);
        assertThat(found.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.LEAVE);
    }

    @Test
    void 학생과_주전공_관계를_저장할_수_있다() {
        UUID test = UUID.randomUUID();

        Student student = studentRepository.saveAndFlush(
            new Student(
                test,
                "student@inha.edu",
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            )
        );

        Department department = departmentRepository.saveAndFlush(new Department("소프트웨어융합공학과"));

        StudentMajor savedStudentMajor = studentMajorRepository.saveAndFlush(
            new StudentMajor(
                student,
                department,
                MajorType.PRIMARY
            )
        );

        Long majorId = savedStudentMajor.getId();
        Long studentId = student.getId();
        Long departmentId = department.getId();

        entityManager.clear();

        StudentMajor found = studentMajorRepository.findById(majorId).orElseThrow();

        assertThat(found.getStudent().getId()).isEqualTo(studentId);
        assertThat(found.getDepartment().getId()).isEqualTo(departmentId);
        assertThat(found.getMajorType()).isEqualTo(MajorType.PRIMARY);
        assertThat(found.getEntityId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void 학생의_활성_주전공을_조회할_수_있다() {
        Student student = studentRepository.saveAndFlush(
            new Student(
                UUID.randomUUID(),
                "primary-major@inha.edu",
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            )
        );
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        studentMajorRepository.saveAndFlush(
            new StudentMajor(student, department, MajorType.PRIMARY)
        );

        entityManager.clear();

        List<StudentMajor> found = studentMajorRepository
            .findAllByStudentAndDeletedAtIsNull(student);

        assertThat(found)
            .singleElement()
            .satisfies(major -> {
                assertThat(major.getMajorType()).isEqualTo(MajorType.PRIMARY);
                assertThat(major.getDepartment().getEntityId())
                    .isEqualTo(department.getEntityId());
            });
    }

    @Test
    void 학생은_활성_주전공을_하나만_가질_수_있다() {
        Student student = studentRepository.saveAndFlush(
            new Student(
                UUID.randomUUID(),
                "student@inha.edu",
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            )
        );

        Department firstDepartment = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );

        Department secondDepartment = departmentRepository.saveAndFlush(
            new Department("산업경영학과")
        );

        studentMajorRepository.saveAndFlush(
            new StudentMajor(
                student,
                firstDepartment,
                MajorType.PRIMARY
            )
        );

        StudentMajor duplicatePrimaryMajor = new StudentMajor(
            student,
            secondDepartment,
            MajorType.PRIMARY
        );

        assertThatThrownBy(
            () -> studentMajorRepository.saveAndFlush(duplicatePrimaryMajor)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학생은_같은_유형의_활성_전공을_두_개_가질_수_없다() {
        Student student = studentRepository.saveAndFlush(new Student(
            UUID.randomUUID(),
            "major-type@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        ));
        Department first = departmentRepository.saveAndFlush(
            new Department("금융투자학과")
        );
        Department second = departmentRepository.saveAndFlush(
            new Department("산업경영학과")
        );

        studentMajorRepository.saveAndFlush(
            new StudentMajor(student, first, MajorType.DOUBLE_MAJOR)
        );

        assertThatThrownBy(() -> studentMajorRepository.saveAndFlush(
            new StudentMajor(student, second, MajorType.DOUBLE_MAJOR)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학생은_같은_학과를_다른_전공_유형으로_중복할_수_없다() {
        Student student = studentRepository.saveAndFlush(new Student(
            UUID.randomUUID(),
            "major-department@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        ));
        Department department = departmentRepository.saveAndFlush(
            new Department("반도체산업융합학과")
        );

        studentMajorRepository.saveAndFlush(
            new StudentMajor(student, department, MajorType.PRIMARY)
        );

        assertThatThrownBy(() -> studentMajorRepository.saveAndFlush(
            new StudentMajor(student, department, MajorType.MINOR)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학생의_활성_전공만_모두_조회한다() {
        Student student = studentRepository.saveAndFlush(new Student(
            UUID.randomUUID(),
            "active-majors@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        ));
        Department primary = departmentRepository.saveAndFlush(
            new Department("금융투자학과")
        );
        Department doubleMajor = departmentRepository.saveAndFlush(
            new Department("산업경영학과")
        );
        Department minorDepartment = departmentRepository.saveAndFlush(
            new Department("메카트로닉스공학과")
        );

        studentMajorRepository.saveAndFlush(
            new StudentMajor(student, primary, MajorType.PRIMARY)
        );
        studentMajorRepository.saveAndFlush(
            new StudentMajor(student, doubleMajor, MajorType.DOUBLE_MAJOR)
        );
        StudentMajor minor = studentMajorRepository.saveAndFlush(
            new StudentMajor(student, minorDepartment, MajorType.MINOR)
        );
        minor.softDelete();
        studentMajorRepository.flush();
        entityManager.clear();

        List<StudentMajor> activeMajors = studentMajorRepository
            .findAllByStudentAndDeletedAtIsNull(student);

        assertThat(activeMajors)
            .extracting(StudentMajor::getMajorType)
            .containsExactlyInAnyOrder(MajorType.PRIMARY, MajorType.DOUBLE_MAJOR);
    }

}
