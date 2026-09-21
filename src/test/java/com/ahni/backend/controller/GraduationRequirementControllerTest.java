package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.RequiredCourseCategory;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GraduationRequirementResponse;
import com.ahni.backend.dto.RequiredCourseResponse;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.service.GraduationRequirementService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GraduationRequirementController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class GraduationRequirementControllerTest {
    private static final UUID AUTH_USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000010"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraduationRequirementService graduationRequirementService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_학생이_전공별_졸업요건을_조회한다() throws Exception {
        when(graduationRequirementService.getRequirements(AUTH_USER_ID))
            .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].majorType").value("PRIMARY"))
            .andExpect(jsonPath("$[0].admissionYear").value(2024))
            .andExpect(jsonPath("$[0].department.name")
                .value("소프트웨어융합공학과"))
            .andExpect(jsonPath("$[0].minTotalCredit").value(130.0))
            .andExpect(jsonPath("$[0].requiredCourses[0].category")
                .value("MAJOR_FOUNDATION"))
            .andExpect(jsonPath("$[0].requiredCourses[0].course.code")
                .value("CSE101"));

        verify(graduationRequirementService).getRequirements(AUTH_USER_ID);
    }

    @Test
    void 학생의_전공에_맞는_졸업요건이_없으면_404를_반환한다() throws Exception {
        when(graduationRequirementService.getRequirements(AUTH_USER_ID))
            .thenThrow(new GraduationRequirementNotFoundException());

        mockMvc.perform(get("/api/v1/graduation-requirements")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code")
                .value("GRADUATION_REQUIREMENT_NOT_FOUND"))
            .andExpect(jsonPath("$.message")
                .value("학생의 입학연도와 전공에 맞는 졸업요건을 찾을 수 없습니다."));
    }

    private GraduationRequirementResponse response() {
        DepartmentResponse department = new DepartmentResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "소프트웨어융합공학과"
        );
        CourseResponse course = new CourseResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR,
            department
        );
        RequiredCourseResponse requiredCourse = new RequiredCourseResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000301"),
            RequiredCourseCategory.MAJOR_FOUNDATION,
            course
        );

        return new GraduationRequirementResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000401"),
            2024,
            "PRIMARY",
            department,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            BigDecimal.ZERO,
            List.of(requiredCourse)
        );
    }
}
