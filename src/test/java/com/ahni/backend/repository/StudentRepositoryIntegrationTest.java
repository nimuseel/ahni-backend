package com.ahni.backend.repository;

import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.entity.*;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
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

        StudentMajor found = studentMajorRepository
            .findByStudentAndMajorTypeAndDeletedAtIsNull(student, MajorType.PRIMARY)
            .orElseThrow();

        assertThat(found.getDepartment().getEntityId()).isEqualTo(department.getEntityId());
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

}
