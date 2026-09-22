package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.dto.CreditProgressResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GraduationCreditProgressResponse;
import com.ahni.backend.dto.GraduationProgressResponse;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.service.GraduationProgressService;
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

@WebMvcTest(GraduationProgressController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class GraduationProgressControllerTest {
    private static final UUID AUTH_USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000010"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraduationProgressService graduationProgressService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_학생이_전공별_학점_충족도를_조회한다() throws Exception {
        when(graduationProgressService.getProgress(AUTH_USER_ID))
            .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/graduation-progress")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].majorType").value("PRIMARY"))
            .andExpect(jsonPath("$[0].department.name")
                .value("소프트웨어융합공학과"))
            .andExpect(jsonPath("$[0].credits.total.required").value(130.0))
            .andExpect(jsonPath("$[0].credits.total.completed").value(42.0))
            .andExpect(jsonPath("$[0].credits.total.remaining").value(88.0))
            .andExpect(jsonPath("$[0].credits.total.met").value(false))
            .andExpect(jsonPath("$[0].credits.department.completed").value(24.0))
            .andExpect(jsonPath("$[0].credits.general.completed").value(12.0));

        verify(graduationProgressService).getProgress(AUTH_USER_ID);
    }

    @Test
    void 인증이_없으면_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/graduation-progress"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 학생의_전공에_맞는_졸업요건이_없으면_404를_반환한다() throws Exception {
        when(graduationProgressService.getProgress(AUTH_USER_ID))
            .thenThrow(new GraduationRequirementNotFoundException());

        mockMvc.perform(get("/api/v1/graduation-progress")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code")
                .value("GRADUATION_REQUIREMENT_NOT_FOUND"));
    }

    private GraduationProgressResponse response() {
        DepartmentResponse department = new DepartmentResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "소프트웨어융합공학과"
        );
        GraduationCreditProgressResponse credits = new GraduationCreditProgressResponse(
            progress("130.0", "42.0", "88.0", false),
            progress("60.0", "24.0", "36.0", false),
            progress("30.0", "12.0", "18.0", false)
        );
        return new GraduationProgressResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000401"),
            2024,
            "PRIMARY",
            department,
            credits
        );
    }

    private CreditProgressResponse progress(
        String required,
        String completed,
        String remaining,
        boolean met
    ) {
        return new CreditProgressResponse(
            new BigDecimal(required),
            new BigDecimal(completed),
            new BigDecimal(remaining),
            met
        );
    }
}
