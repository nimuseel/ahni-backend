package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Tag("integration")
class GraduationRequirementRepositoryIntegrationTest {
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
    private GraduationRequirementRepository graduationRequirementRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearRequirements() {
        graduationRequirementRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    @Test
    void 학과와_입학연도와_전공유형으로_졸업요건을_조회한다() {
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        GraduationRequirement requirement = graduationRequirementRepository.saveAndFlush(
            requirement(department, 2024, MajorType.PRIMARY)
        );

        entityManager.clear();

        GraduationRequirement found = graduationRequirementRepository
            .findByDepartmentAndAdmissionYearAndMajorType(
                department,
                2024,
                MajorType.PRIMARY
            )
            .orElseThrow();

        assertThat(found.getEntityId()).isEqualTo(requirement.getEntityId());
        assertThat(found.getMinTotalCredit()).isEqualByComparingTo("130.0");
        assertThat(found.getMinDepartmentCredit()).isEqualByComparingTo("60.0");
        assertThat(found.getMinGeneralCredit()).isEqualByComparingTo("30.0");
    }

    @Test
    void 학과의_졸업요건을_입학연도_내림차순으로_조회한다() {
        Department department = departmentRepository.saveAndFlush(
            new Department("소프트웨어융합공학과")
        );
        graduationRequirementRepository.saveAllAndFlush(List.of(
            requirement(department, 2023, MajorType.PRIMARY),
            requirement(department, 2024, MajorType.PRIMARY)
        ));

        List<GraduationRequirement> requirements = graduationRequirementRepository
            .findAllByDepartmentOrderByAdmissionYearDescMajorTypeAsc(department);

        assertThat(requirements)
            .extracting(GraduationRequirement::getAdmissionYear)
            .containsExactly(2024, 2023);
    }

    private GraduationRequirement requirement(
        Department department,
        int admissionYear,
        MajorType majorType
    ) {
        return new GraduationRequirement(
            department,
            admissionYear,
            majorType,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            new BigDecimal("30.0")
        );
    }
}
