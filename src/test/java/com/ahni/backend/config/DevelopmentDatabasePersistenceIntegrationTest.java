package com.ahni.backend.config;

import com.ahni.backend.entity.Department;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class DevelopmentDatabasePersistenceIntegrationTest {
    @Test
    void development_schema_initialization_preserves_records_after_restart() throws IOException {
        try (var postgres = new PostgreSQLContainer<>("postgres:18")) {
            postgres.start();
            var department = new Department("소프트웨어융합공학과");

            try (var firstStartup = openPersistence(postgres)) {
                firstStartup.inTransaction(session -> session.persist(department));
            }

            try (var restarted = openPersistence(postgres)) {
                var storedDepartments = restarted.fromTransaction(session ->
                    session.createSelectionQuery(
                        "from Department where entityId = :entityId", Department.class
                    ).setParameter("entityId", department.getEntityId()).getResultList()
                );
                assertThat(storedDepartments)
                    .extracting(Department::getName)
                    .containsExactly("소프트웨어융합공학과");
            }
        }
    }

    private SessionFactory openPersistence(PostgreSQLContainer<?> postgres) throws IOException {
        var properties = new Properties();
        try (var resource = Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("application-dev.properties")
        )) {
            properties.load(resource);
        }
        return new Configuration()
            .addAnnotatedClass(Department.class)
            .setProperty("hibernate.connection.url", postgres.getJdbcUrl())
            .setProperty("hibernate.connection.username", postgres.getUsername())
            .setProperty("hibernate.connection.password", postgres.getPassword())
            .setProperty("hibernate.hbm2ddl.auto", properties.getProperty("spring.jpa.hibernate.ddl-auto"))
            .buildSessionFactory();
    }
}
