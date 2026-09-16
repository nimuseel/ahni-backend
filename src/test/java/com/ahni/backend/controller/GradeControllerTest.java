package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GradeCourseResponse;
import com.ahni.backend.dto.GradeRegistrationRequest;
import com.ahni.backend.dto.GradeResponse;
import com.ahni.backend.exception.CourseNotFoundException;
import com.ahni.backend.exception.GradeAlreadyRegisteredException;
import com.ahni.backend.exception.InvalidGradeException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.service.GradeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class GradeControllerTest {
    private static final UUID AUTH_USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000010"
    );
    private static final UUID COURSE_ENTITY_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000101"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeService gradeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_학생이_성적을_등록한다() throws Exception {
        GradeRegistrationRequest request = standardRequest();
        when(gradeService.register(AUTH_USER_ID, request))
            .thenReturn(response(GradeCode.A_PLUS, new BigDecimal("4.50"), false));

        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(standardRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entityId").value(
                "00000000-0000-0000-0000-000000000201"
            ))
            .andExpect(jsonPath("$.course.code").value("CSE101"))
            .andExpect(jsonPath("$.gradePoint").value(4.50))
            .andExpect(jsonPath("$.credit").value(3.0));

        verify(gradeService).register(AUTH_USER_ID, request);
    }

    @Test
    void RPL_성적은_등급과_평점을_null로_응답한다() throws Exception {
        GradeRegistrationRequest request = new GradeRegistrationRequest(
            COURSE_ENTITY_ID,
            2025,
            AcademicTerm.SECOND,
            null,
            new BigDecimal("3.0"),
            true,
            false
        );
        when(gradeService.register(AUTH_USER_ID, request))
            .thenReturn(response(null, null, true));

        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseEntityId": "00000000-0000-0000-0000-000000000101",
                      "academicYear": 2025,
                      "term": "SECOND",
                      "gradeCode": null,
                      "credit": 3.0,
                      "rpl": true,
                      "retake": false
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gradeCode").value(nullValue()))
            .andExpect(jsonPath("$.gradePoint").value(nullValue()))
            .andExpect(jsonPath("$.rpl").value(true));
    }

    @Test
    void 인증된_학생이_자신의_성적_목록을_조회한다() throws Exception {
        when(gradeService.getGrades(AUTH_USER_ID)).thenReturn(List.of(
            response(GradeCode.A_PLUS, new BigDecimal("4.50"), false)
        ));

        mockMvc.perform(get("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].course.code").value("CSE101"))
            .andExpect(jsonPath("$[0].academicYear").value(2025))
            .andExpect(jsonPath("$[0].term").value("SECOND"));

        verify(gradeService).getGrades(AUTH_USER_ID);
    }

    @Test
    void 등록된_성적이_없으면_빈_목록을_응답한다() throws Exception {
        when(gradeService.getGrades(AUTH_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void 인증되지_않은_사용자는_성적을_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(standardRequestJson()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(gradeService);
    }

    @Test
    void 인증되지_않은_사용자는_성적을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/grades"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(gradeService);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void 잘못된_성적_요청은_400을_반환한다(String requestJson) throws Exception {
        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(gradeService);
    }

    static Stream<String> invalidRequests() {
        return Stream.of(
            "{",
            """
                {"courseEntityId":"not-a-uuid","academicYear":2025,"term":"SECOND","credit":3.0}
                """,
            """
                {"academicYear":2025,"term":"SECOND","gradeCode":"A_PLUS","credit":3.0}
                """,
            """
                {"courseEntityId":"00000000-0000-0000-0000-000000000101","term":"SECOND","gradeCode":"A_PLUS","credit":3.0}
                """,
            """
                {"courseEntityId":"00000000-0000-0000-0000-000000000101","academicYear":2025,"gradeCode":"A_PLUS","credit":3.0}
                """,
            """
                {"courseEntityId":"00000000-0000-0000-0000-000000000101","academicYear":2025,"term":"INVALID","gradeCode":"A_PLUS","credit":3.0}
                """,
            """
                {"courseEntityId":"00000000-0000-0000-0000-000000000101","academicYear":2025,"term":"SECOND","gradeCode":"A_PLUS"}
                """,
            """
                {"courseEntityId":"00000000-0000-0000-0000-000000000101","academicYear":2025,"term":"SECOND","gradeCode":"A_PLUS","credit":3.25}
                """
        );
    }

    @Test
    void 도메인_성적_오류는_400과_안정적인_코드를_반환한다() throws Exception {
        when(gradeService.register(AUTH_USER_ID, standardRequest()))
            .thenThrow(new InvalidGradeException("일반 성적에는 등급이 필수입니다."));

        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(standardRequestJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_GRADE"))
            .andExpect(jsonPath("$.message").value("일반 성적에는 등급이 필수입니다."));
    }

    @Test
    void 학생_프로필이_없으면_404를_반환한다() throws Exception {
        when(gradeService.getGrades(AUTH_USER_ID)).thenThrow(new StudentNotFoundException());

        mockMvc.perform(get("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void 과목이_없으면_404를_반환한다() throws Exception {
        when(gradeService.register(AUTH_USER_ID, standardRequest()))
            .thenThrow(new CourseNotFoundException());

        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(standardRequestJson()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void 중복_성적은_409를_반환한다() throws Exception {
        when(gradeService.register(AUTH_USER_ID, standardRequest()))
            .thenThrow(new GradeAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/grades")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(standardRequestJson()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GRADE_ALREADY_REGISTERED"));
    }

    private GradeRegistrationRequest standardRequest() {
        return new GradeRegistrationRequest(
            COURSE_ENTITY_ID,
            2025,
            AcademicTerm.SECOND,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            false
        );
    }

    private String standardRequestJson() {
        return """
            {
              "courseEntityId": "00000000-0000-0000-0000-000000000101",
              "academicYear": 2025,
              "term": "SECOND",
              "gradeCode": "A_PLUS",
              "credit": 3.0,
              "rpl": false,
              "retake": false
            }
            """;
    }

    private GradeResponse response(GradeCode gradeCode, BigDecimal gradePoint, boolean rpl) {
        return new GradeResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000201"),
            new GradeCourseResponse(
                COURSE_ENTITY_ID,
                "CSE101",
                "프로그래밍 기초",
                CourseCategory.MAJOR,
                new DepartmentResponse(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "소프트웨어융합공학과"
                )
            ),
            2025,
            AcademicTerm.SECOND,
            gradeCode,
            gradePoint,
            new BigDecimal("3.0"),
            rpl,
            false,
            Instant.parse("2026-09-16T00:00:00Z"),
            Instant.parse("2026-09-16T00:00:00Z")
        );
    }
}
