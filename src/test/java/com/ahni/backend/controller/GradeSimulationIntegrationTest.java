package com.ahni.backend.controller;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:grade_simulation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class GradeSimulationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void 본인_성적만_합산하고_실제_성적은_변경하지_않는다() throws Exception {
        Student owner = student("owner@inha.edu");
        Student other = student("other@inha.edu");
        Course course = new Course(null, "ELE101", "테스트 과목",
            new BigDecimal("3.0"), CourseCategory.ELECTIVE);
        entityManager.persist(course);
        entityManager.persist(new StudentGrade(owner, course, 2025,
            AcademicTerm.FIRST, GradeCode.B_ZERO, new BigDecimal("3.0"), false, null));
        entityManager.persist(new StudentGrade(other, course, 2025,
            AcademicTerm.FIRST, GradeCode.A_PLUS, new BigDecimal("3.0"), false, null));
        entityManager.flush();
        entityManager.clear();
        var before = jdbc.queryForList("select * from student_grade order by id");

        mvc.perform(post("/api/v1/grades/simulation")
                .with(jwt().jwt(token -> token.subject(owner.getAuthUserId().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"expectedGrades":[{"category":"MAJOR","credit":3.0,"gradeCode":"A_PLUS"}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current.gpa").value(3.0))
            .andExpect(jsonPath("$.current.gpaCredits").value(3.0))
            .andExpect(jsonPath("$.projected.gpa").value(3.75))
            .andExpect(jsonPath("$.projected.completedCredits").value(6.0))
            .andExpect(jsonPath("$.projected.categories[0].gpa").value(4.5));

        entityManager.flush();
        entityManager.clear();
        assertThat(jdbc.queryForList("select * from student_grade order by id"))
            .isEqualTo(before);
    }

    @Test
    void 성적이_없는_학생도_예상_성적을_계산한다() throws Exception {
        Student owner = student("empty@inha.edu");
        mvc.perform(post("/api/v1/grades/simulation")
                .with(jwt().jwt(token -> token.subject(owner.getAuthUserId().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"expectedGrades":[{"category":"ELECTIVE","credit":0.5,"gradeCode":"A_ZERO"}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current.gpa").value(0.0))
            .andExpect(jsonPath("$.projected.gpa").value(4.0))
            .andExpect(jsonPath("$.projected.gpaCredits").value(0.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"expectedGrades\":null}", "{\"expectedGrades\":[]}",
        "{\"expectedGrades\":[null]}",
        "{\"expectedGrades\":[{}]}",
        "{\"expectedGrades\":[{\"category\":\"UNKNOWN\",\"credit\":3,\"gradeCode\":\"A_PLUS\"}]}",
        "{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":3,\"gradeCode\":\"UNKNOWN\"}]}",
        "{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":0,\"gradeCode\":\"F\"}]}",
        "{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":-1,\"gradeCode\":\"F\"}]}",
        "{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":30.1,\"gradeCode\":\"P\"}]}",
        "{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":1.25,\"gradeCode\":\"NP\"}]}"
    })
    void 유효하지_않은_예상_성적은_안정된_입력_오류를_반환한다(String body) throws Exception {
        mvc.perform(post("/api/v1/grades/simulation")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 예상_성적_개수에_상한을_둔다() throws Exception {
        String row = "{\"category\":\"MAJOR\",\"credit\":3,\"gradeCode\":\"A_PLUS\"}";
        String body = "{\"expectedGrades\":[" + String.join(",", java.util.Collections.nCopies(51, row)) + "]}";
        mvc.perform(post("/api/v1/grades/simulation").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 학생_프로필이_없으면_계산하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/grades/simulation")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedGrades\":[{\"category\":\"MAJOR\",\"credit\":3,\"gradeCode\":\"F\"}]}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void 인증_없이_계산할_수_없다() throws Exception {
        mvc.perform(post("/api/v1/grades/simulation")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    private Student student(String email) {
        Student student = new Student(UUID.randomUUID(), email, 2024,
            EnrollmentStatus.ENROLLED, null);
        entityManager.persist(student);
        return student;
    }
}
