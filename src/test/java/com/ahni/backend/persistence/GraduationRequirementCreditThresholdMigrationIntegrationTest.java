package com.ahni.backend.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

@Tag("integration")
class GraduationRequirementCreditThresholdMigrationIntegrationTest {

    @Test
    void 복수전공_학점기준을_해당_전공_학점으로_이전한다() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")) {
            postgres.start();
            migrate(postgres, MigrationVersion.fromVersion("15"));

            try (Connection connection = connection(postgres);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO public.department (name)
                    VALUES ('마이그레이션검증학과')
                    """);
                statement.executeUpdate("""
                    INSERT INTO public.graduation_requirement (
                        department_id,
                        admission_year,
                        major_type,
                        min_total_credit,
                        min_major_credit,
                        min_double_major_credit
                    ) VALUES (
                        (SELECT id FROM public.department WHERE name = '마이그레이션검증학과'),
                        2024,
                        'DOUBLE_MAJOR',
                        130.0,
                        60.0,
                        42.0
                    )
                    """);
            }

            migrate(postgres, null);

            try (Connection connection = connection(postgres);
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                     SELECT min_department_credit, min_general_credit
                     FROM public.graduation_requirement
                     WHERE major_type = 'DOUBLE_MAJOR'
                     """)) {
                result.next();
                assertAll(
                    () -> assertEquals(
                        0,
                        new BigDecimal("42.0").compareTo(
                            result.getBigDecimal("min_department_credit")
                        )
                    ),
                    () -> assertEquals(
                        0,
                        BigDecimal.ZERO.compareTo(
                            result.getBigDecimal("min_general_credit")
                        )
                    )
                );
            }

            try (Connection connection = connection(postgres);
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                     SELECT count(*)
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name = 'graduation_requirement'
                       AND column_name IN ('min_major_credit', 'min_double_major_credit')
                     """)) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
        }
    }

    private static void migrate(
        PostgreSQLContainer<?> postgres,
        MigrationVersion target
    ) {
        var configuration = Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private static Connection connection(
        PostgreSQLContainer<?> postgres
    ) throws SQLException {
        return DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
    }
}
