package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
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
}