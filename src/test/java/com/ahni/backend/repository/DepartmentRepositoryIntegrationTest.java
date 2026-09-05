package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import jakarta.persistence.EntityManager;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Tag("integration")
class DepartmentRepositoryIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

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
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 학과를_저장한다() {
        Department department = new Department("소프트웨어융합공학과");

        Department saved = departmentRepository.saveAndFlush(department);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEntityId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void entityId로_학과를_조회할_수_있다() {
        Department department = departmentRepository.saveAndFlush(new Department("산업경영학과"));
        UUID entityId = department.getEntityId();

        entityManager.clear();

        Department found = departmentRepository.findByEntityId(entityId).orElseThrow();

        assertThat(found.getEntityId()).isEqualTo(entityId);
    }

    @Test
    void 동일한_이름의_학과는_저장할_수_없다() {
        departmentRepository.saveAndFlush(new Department("소프트웨어융합공학과"));
        assertThatThrownBy(() -> departmentRepository.saveAndFlush(new Department("소프트웨어융합공학과")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 학과를_수정하면_updateAt이_갱신된다() {
        Department department = departmentRepository.saveAndFlush(new Department("소프트웨어융합공학과"));

        entityManager.clear();

        Department found = departmentRepository
            .findByEntityId(department.getEntityId())
            .orElseThrow();

        Instant createdAt = found.getCreatedAt();
        Instant previousUpdatedAt = found.getUpdatedAt();

        found.rename("산업경영학과");
        departmentRepository.saveAndFlush(found);

        entityManager.clear();

        Department updated = departmentRepository.findByEntityId(found.getEntityId()).orElseThrow();

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void 삭제되지_않은_학과를_이름순으로_조회한다() {
        departmentRepository.saveAllAndFlush(List.of(
            new Department("소프트웨어융합공학과"),
            new Department("산업경영학과"),
            new Department("메카트로닉스공학과"),
            new Department("금융투자학과"),
            new Department("반도체산업융합학과")
        ));

        entityManager.clear();

        List<Department> departments = departmentRepository.findAllByDeletedAtIsNullOrderByNameAsc();

        assertThat(departments)
            .extracting(Department::getName)
            .containsExactly(
                "금융투자학과",
                "메카트로닉스공학과",
                "반도체산업융합학과",
                "산업경영학과",
                "소프트웨어융합공학과"
            );
    }
}