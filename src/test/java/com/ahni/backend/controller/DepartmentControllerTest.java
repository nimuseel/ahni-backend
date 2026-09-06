package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.service.DepartmentService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class DepartmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 학과_목록을_조회한다() throws Exception {
        UUID financeId =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID softwareId =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(departmentService.getDepartments())
            .thenReturn(List.of(
                new DepartmentResponse(financeId, "금융투자학과"),
                new DepartmentResponse(softwareId, "소프트웨어융합공학과")
            ));

        mockMvc.perform(get("/api/v1/departments"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$[0].entityId").value(financeId.toString()))
            .andExpect(jsonPath("$[0].name").value("금융투자학과"))
            .andExpect(jsonPath("$[1].entityId").value(softwareId.toString()))
            .andExpect(jsonPath("$[1].name").value("소프트웨어융합공학과"));

    }
}