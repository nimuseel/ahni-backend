package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.InvalidCourseCategoryException;
import com.ahni.backend.service.CourseService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class CourseControllerTest {
    private static final UUID DEPARTMENT_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MAJOR_COURSE_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID GENERAL_COURSE_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_사용자가_필터로_과목_목록을_조회한다() throws Exception {
        when(courseService.getCourses(DEPARTMENT_ID, "major"))
            .thenReturn(List.of(
                new CourseResponse(
                    MAJOR_COURSE_ID,
                    "CSE101",
                    "프로그래밍 기초",
                    new BigDecimal("3.0"),
                    CourseCategory.MAJOR,
                    new DepartmentResponse(DEPARTMENT_ID, "소프트웨어융합공학과")
                ),
                new CourseResponse(
                    GENERAL_COURSE_ID,
                    "GE101",
                    "대학 글쓰기",
                    new BigDecimal("2.0"),
                    CourseCategory.GENERAL_EDUCATION,
                    null
                )
            ));

        mockMvc.perform(get("/api/v1/courses")
                .param("departmentEntityId", DEPARTMENT_ID.toString())
                .param("category", "major")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$[0].entityId").value(MAJOR_COURSE_ID.toString()))
            .andExpect(jsonPath("$[0].code").value("CSE101"))
            .andExpect(jsonPath("$[0].name").value("프로그래밍 기초"))
            .andExpect(jsonPath("$[0].credit").value(3.0))
            .andExpect(jsonPath("$[0].category").value("MAJOR"))
            .andExpect(jsonPath("$[0].department.entityId").value(DEPARTMENT_ID.toString()))
            .andExpect(jsonPath("$[0].department.name").value("소프트웨어융합공학과"))
            .andExpect(jsonPath("$[1].category").value("GENERAL_EDUCATION"))
            .andExpect(jsonPath("$[1].department").doesNotExist());

        verify(courseService).getCourses(DEPARTMENT_ID, "major");
    }

    @Test
    void 조회된_과목이_없으면_빈_배열을_반환한다() throws Exception {
        when(courseService.getCourses(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void 인증되지_않은_사용자는_과목을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void 유효하지_않은_분류이면_400을_반환한다() throws Exception {
        when(courseService.getCourses(null, "required"))
            .thenThrow(new InvalidCourseCategoryException());

        mockMvc.perform(get("/api/v1/courses")
                .param("category", "required")
                .with(jwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COURSE_CATEGORY"))
            .andExpect(jsonPath("$.message").value("과목 분류가 올바르지 않습니다."));
    }

    @Test
    void 학과_식별자_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                .param("departmentEntityId", "not-a-uuid")
                .with(jwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(courseService);
    }

    @Test
    void 존재하지_않는_학과이면_404를_반환한다() throws Exception {
        when(courseService.getCourses(DEPARTMENT_ID, null))
            .thenThrow(new DepartmentNotFoundException());

        mockMvc.perform(get("/api/v1/courses")
                .param("departmentEntityId", DEPARTMENT_ID.toString())
                .with(jwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }
}
