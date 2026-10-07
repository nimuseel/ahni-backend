package com.ahni.backend.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class SharedMajorMigrationIntegrationTest {
    @Test
    void v20PreservesExistingAffiliationAndOnlyRemovesTheMajorDepartmentCheck() throws SQLException {
        try (var postgres = new PostgreSQLContainer<>("postgres:18")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("19").load().migrate();
            String existing;
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO course(code,name,credit,category,department_id) SELECT 'SHARED101','공통 전공',3.0,'MAJOR',min(id) FROM department");
                try (var row = sql.executeQuery("SELECT row_to_json(c)::text FROM course c WHERE code='SHARED101'")) {
                    assertTrue(row.next());
                    existing = row.getString(1);
                }
                assertEquals("23514", assertThrows(SQLException.class,
                    () -> sql.executeUpdate("INSERT INTO course(code,name,credit,category) VALUES ('SHARED102','공통 전공2',3.0,'MAJOR')")).getSQLState());
            }
            var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            flyway.validate();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var sql = connection.createStatement()) {
                try (var row = sql.executeQuery("SELECT row_to_json(c)::text FROM course c WHERE code='SHARED101'")) {
                    assertTrue(row.next());
                    assertEquals(existing, row.getString(1));
                }
                assertEquals(1, sql.executeUpdate("INSERT INTO course(code,name,credit,category) VALUES ('SHARED102','공통 전공2',3.0,'MAJOR')"));
                for (var statement : new String[] {
                    "INSERT INTO course(code,name,credit,category) VALUES ('BAD101','오류',3.0,'UNKNOWN')",
                    "INSERT INTO course(code,name,credit,category) VALUES ('bad101','오류',3.0,'MAJOR')",
                    "INSERT INTO course(code,name,credit,category) VALUES ('BAD101','오류',30.1,'MAJOR')"
                }) assertEquals("23514", assertThrows(SQLException.class, () -> sql.executeUpdate(statement)).getSQLState());
                assertEquals("23505", assertThrows(SQLException.class,
                    () -> sql.executeUpdate("INSERT INTO course(code,name,credit,category) VALUES ('SHARED102','중복',3.0,'MAJOR')")).getSQLState());
                assertEquals("23503", assertThrows(SQLException.class,
                    () -> sql.executeUpdate("INSERT INTO course(code,name,credit,category,department_id) VALUES ('BAD102','없는학과',3.0,'MAJOR',-1)")).getSQLState());
            }
        }
    }
}
