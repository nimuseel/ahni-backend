package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.RequiredCourseCategory;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GraduationRequirementResponse;
import com.ahni.backend.dto.RequiredCourseResponse;
import com.ahni.backend.exception.AdminAccessDeniedException;
import com.ahni.backend.service.GraduationRequirementManagementService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GraduationRequirementManagementController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class GraduationRequirementManagementControllerTest {
    private static final UUID ADMIN_AUTH_USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000901"
    );
    private static final UUID REQUIREMENT_ENTITY_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000401"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraduationRequirementManagementService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 졸업요건과_필수과목을_등록한다() throws Exception {
        when(service.create(eq(ADMIN_AUTH_USER_ID), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/admin/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entityId")
                .value(REQUIREMENT_ENTITY_ID.toString()))
            .andExpect(jsonPath("$.sourceTitle")
                .value("2024학년도 졸업요건"))
            .andExpect(jsonPath("$.requiredCourses[0].course.code")
                .value("CSE101"));
    }

    @Test
    void 필터에_맞는_졸업요건_목록을_조회한다() throws Exception {
        UUID departmentEntityId = UUID.fromString(
            "00000000-0000-0000-0000-000000000001"
        );
        when(service.findAll(
            ADMIN_AUTH_USER_ID,
            departmentEntityId,
            2024,
            "PRIMARY"
        )).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/admin/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .queryParam("departmentEntityId", departmentEntityId.toString())
                .queryParam("admissionYear", "2024")
                .queryParam("majorType", "PRIMARY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].entityId")
                .value(REQUIREMENT_ENTITY_ID.toString()))
            .andExpect(jsonPath("$[0].department.name")
                .value("소프트웨어융합공학과"))
            .andExpect(jsonPath("$[0].requiredCourses[0].course.code")
                .value("CSE101"));
    }

    @Test
    void 졸업요건과_필수과목_구성을_수정한다() throws Exception {
        when(service.update(eq(ADMIN_AUTH_USER_ID), any(), any()))
            .thenReturn(response());

        mockMvc.perform(put(
                "/api/v1/admin/graduation-requirements/{requirementEntityId}",
                REQUIREMENT_ENTITY_ID
            )
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minTotalCredit").value(130.0))
            .andExpect(jsonPath("$.sourceUrl")
                .value("https://example.edu/requirements/2024"));
    }

    @Test
    void 인증되지_않은_요청은_졸업요건을_등록할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/graduation-requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 필수값이_누락된_요청은_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "admissionYear": 2024,
                      "majorType": "PRIMARY",
                      "minTotalCredit": 130.0,
                      "minDepartmentCredit": 60.0,
                      "minGeneralCredit": 30.0,
                      "sourceTitle": "",
                      "requiredCourses": []
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 활성_관리자가_아닌_요청은_403을_반환한다() throws Exception {
        when(service.create(eq(ADMIN_AUTH_USER_ID), any()))
            .thenThrow(new AdminAccessDeniedException());

        mockMvc.perform(post("/api/v1/admin/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
    }

    @Test
    void 출처_URL_형식이_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(ADMIN_AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest().replace(
                    "https://example.edu/requirements/2024",
                    "not-a-url"
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private String createRequest() {
        return """
            {
              "departmentEntityId": "00000000-0000-0000-0000-000000000001",
              "admissionYear": 2024,
              "majorType": "PRIMARY",
              "minTotalCredit": 130.0,
              "minDepartmentCredit": 60.0,
              "minGeneralCredit": 30.0,
              "sourceTitle": "2024학년도 졸업요건",
              "sourceUrl": "https://example.edu/requirements/2024",
              "requiredCourses": [
                {
                  "courseEntityId": "00000000-0000-0000-0000-000000000101",
                  "category": "MAJOR_FOUNDATION"
                }
              ]
            }
            """;
    }

    private String updateRequest() {
        return """
            {
              "minTotalCredit": 130.0,
              "minDepartmentCredit": 60.0,
              "minGeneralCredit": 30.0,
              "sourceTitle": "2024학년도 졸업요건",
              "sourceUrl": "https://example.edu/requirements/2024",
              "requiredCourses": [
                {
                  "courseEntityId": "00000000-0000-0000-0000-000000000101",
                  "category": "MAJOR_FOUNDATION"
                }
              ]
            }
            """;
    }

    private GraduationRequirementResponse response() {
        DepartmentResponse department = new DepartmentResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "소프트웨어융합공학과"
        );
        RequiredCourseResponse requiredCourse = new RequiredCourseResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000301"),
            RequiredCourseCategory.MAJOR_FOUNDATION,
            new CourseResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "CSE101",
                "프로그래밍 기초",
                new BigDecimal("3.0"),
                CourseCategory.MAJOR,
                department
            )
        );
        return new GraduationRequirementResponse(
            REQUIREMENT_ENTITY_ID,
            2024,
            "PRIMARY",
            department,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            new BigDecimal("30.0"),
            "2024학년도 졸업요건",
            "https://example.edu/requirements/2024",
            List.of(requiredCourse)
        );
    }
}
