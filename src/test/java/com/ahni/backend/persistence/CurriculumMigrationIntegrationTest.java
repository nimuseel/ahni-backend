package com.ahni.backend.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CurriculumMigrationIntegrationTest {
    @Test
    void blocksDirectClientAccessWhileAllowingBackendRole() throws SQLException {
        try (var postgres = new PostgreSQLContainer<>("postgres:18")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load().migrate();
            try (Connection connection = connection(postgres); Statement sql = connection.createStatement()) {
                sql.executeUpdate("CREATE ROLE anon NOLOGIN");
                sql.executeUpdate("CREATE ROLE authenticated NOLOGIN");
                sql.executeUpdate("CREATE ROLE ahni_backend_probe NOLOGIN BYPASSRLS");
                sql.executeUpdate("GRANT USAGE ON SCHEMA public TO anon, authenticated, ahni_backend_probe");
                sql.executeUpdate("GRANT ALL ON curriculum, curriculum_course TO anon, authenticated, ahni_backend_probe");
                sql.executeUpdate("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, ahni_backend_probe");
                sql.executeUpdate("INSERT INTO course(code, name, credit, category) VALUES ('RLS101', '접근 검증 과목', 3.0, 'ELECTIVE')");
                long departmentId = scalar(sql, "SELECT min(id) FROM department");
                sql.executeUpdate("INSERT INTO curriculum(department_id, curriculum_year, source_title) VALUES (" + departmentId + ", 2024, '공식 자료')");
                long curriculumId = scalar(sql, "SELECT id FROM curriculum");
                long courseId = scalar(sql, "SELECT id FROM course");
                sql.executeUpdate("INSERT INTO curriculum_course(curriculum_id, course_id, division) VALUES (" + curriculumId + ", " + courseId + ", 'ELECTIVE')");
                for (String role : new String[]{"anon", "authenticated"}) {
                    sql.execute("SET ROLE " + role);
                    assertEquals(0, scalar(sql, "SELECT count(*) FROM curriculum"), role);
                    assertEquals(0, scalar(sql, "SELECT count(*) FROM curriculum_course"), role);
                    assertSqlState(sql, "42501", "INSERT INTO curriculum(department_id, curriculum_year, source_title) VALUES (" + departmentId + ", 2025, '차단 대상')");
                    assertSqlState(sql, "42501", "INSERT INTO curriculum_course(curriculum_id, course_id, division) VALUES (" + curriculumId + ", " + courseId + ", 'ELECTIVE')");
                    assertEquals(0, sql.executeUpdate("UPDATE curriculum SET source_title = '무단 수정'"));
                    assertEquals(0, sql.executeUpdate("UPDATE curriculum_course SET note = '무단 수정'"));
                    assertEquals(0, sql.executeUpdate("DELETE FROM curriculum_course"));
                    assertEquals(0, sql.executeUpdate("DELETE FROM curriculum"));
                    sql.execute("RESET ROLE");
                }
                sql.execute("SET ROLE ahni_backend_probe");
                assertEquals(1, scalar(sql, "SELECT count(*) FROM curriculum"));
                assertEquals(1, scalar(sql, "SELECT count(*) FROM curriculum_course"));
                assertEquals(1, sql.executeUpdate("UPDATE curriculum SET source_title = '백엔드 수정'"));
                assertEquals(1, sql.executeUpdate("UPDATE curriculum_course SET note = '백엔드 수정'"));
                assertEquals(1, sql.executeUpdate("INSERT INTO curriculum(department_id, curriculum_year, source_title) VALUES (" + departmentId + ", 2025, '백엔드 추가')"));
                long addedId = scalar(sql, "SELECT id FROM curriculum WHERE curriculum_year = 2025");
                assertEquals(1, sql.executeUpdate("INSERT INTO curriculum_course(curriculum_id, course_id, division) VALUES (" + addedId + ", " + courseId + ", 'ELECTIVE')"));
                assertEquals(1, sql.executeUpdate("DELETE FROM curriculum_course WHERE curriculum_id = " + addedId));
                assertEquals(1, sql.executeUpdate("DELETE FROM curriculum WHERE id = " + addedId));
                sql.execute("RESET ROLE");
            }
        }
    }

    @Test
    void preservesExistingGradesAndEnforcesCurriculumConstraints() throws SQLException {
        try (var postgres = new PostgreSQLContainer<>("postgres:18")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("17").load().migrate();
            UUID courseId = UUID.randomUUID();
            UUID gradeId = UUID.randomUUID();
            try (Connection connection = connection(postgres); Statement sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO course(entity_id, code, name, credit, category) VALUES ('"
                    + courseId + "', 'TEST101', '보존 과목', 3.0, 'ELECTIVE')");
                sql.executeUpdate("""
                    INSERT INTO student(auth_user_id, email, admission_year, enrollment_status)
                    VALUES (gen_random_uuid(), 'migration@inha.edu', 2024, 'ENROLLED')
                    """);
                sql.executeUpdate("INSERT INTO student_grade(entity_id, student_id, course_id, academic_year, term, grade_code, grade_point, credit) "
                    + "SELECT '" + gradeId + "', s.id, c.id, 2024, 'FIRST', 'A_PLUS', 4.50, 3.0 FROM student s CROSS JOIN course c");
            }
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load().migrate();
            try (Connection connection = connection(postgres); Statement sql = connection.createStatement()) {
                sql.executeUpdate("""
                    INSERT INTO curriculum(department_id, curriculum_year, source_title)
                    SELECT id, 2024, '공식 교과과정' FROM department ORDER BY id LIMIT 1
                    """);
                assertSqlState(sql, "23505", "INSERT INTO curriculum(department_id, curriculum_year, source_title) SELECT department_id, curriculum_year, source_title FROM curriculum");
                assertSqlState(sql, "23514", "INSERT INTO curriculum(department_id, curriculum_year, source_title) SELECT department_id, 1999, source_title FROM curriculum");
                assertSqlState(sql, "23503", "INSERT INTO curriculum(department_id, curriculum_year, source_title) VALUES (-1, 2024, '자료')");
                sql.executeUpdate("INSERT INTO curriculum_course(curriculum_id, course_id, division) SELECT cr.id, c.id, 'ELECTIVE' FROM curriculum cr CROSS JOIN course c");
                assertSqlState(sql, "23505", "INSERT INTO curriculum_course(curriculum_id, course_id, division) SELECT curriculum_id, course_id, division FROM curriculum_course");
                assertSqlState(sql, "23514", "UPDATE curriculum_course SET division = 'INVALID'");
                assertSqlState(sql, "23514", "UPDATE curriculum_course SET recommended_year = 0");
                assertSqlState(sql, "23514", "UPDATE curriculum_course SET recommended_term = 'WINTER'");
                assertSqlState(sql, "23503", "DELETE FROM course");
                sql.executeUpdate("DELETE FROM curriculum_course");
                sql.executeUpdate("DELETE FROM curriculum");
                try (var result = sql.executeQuery("SELECT g.entity_id, c.entity_id, g.credit, g.grade_point FROM student_grade g JOIN course c ON c.id = g.course_id")) {
                    assertTrue(result.next());
                    assertEquals(gradeId, result.getObject(1, UUID.class));
                    assertEquals(courseId, result.getObject(2, UUID.class));
                    assertEquals("3.0", result.getBigDecimal(3).toPlainString());
                    assertEquals("4.50", result.getBigDecimal(4).toPlainString());
                    assertFalse(result.next());
                }
            }
        }
    }

    private static void assertSqlState(Statement statement, String state, String sql) {
        assertEquals(state, assertThrows(SQLException.class, () -> statement.executeUpdate(sql)).getSQLState());
    }

    private static long scalar(Statement statement, String query) throws SQLException {
        try (var result = statement.executeQuery(query)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static Connection connection(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
