package com.ahni.backend.controller;
import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.dto.*;
import com.ahni.backend.exception.*;
import com.ahni.backend.service.GraduationRequirementImpactService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(GraduationRequirementImpactController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class GraduationRequirementImpactControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GraduationRequirementImpactService service;
    @MockitoBean JwtDecoder decoder;
    final UUID auth = UUID.randomUUID();
    final UUID policy = UUID.randomUUID();
    @Test
    void returns_only_aggregate_impact_for_an_administrator() throws Exception {
        when(service.getImpact(auth, policy)).thenReturn(new GraduationRequirementImpactResponse(policy, 2024, "PRIMARY", new DepartmentResponse(UUID.randomUUID(), "학과"), 7));
        mvc.perform(get("/api/v1/admin/graduation-requirements/" + policy + "/impact").with(jwt().jwt(t -> t.subject(auth.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.affectedStudentCount").value(7)).andExpect(jsonPath("$.students").doesNotExist());
    }
    @Test
    void anonymous_non_admin_and_missing_policy_have_stable_failures() throws Exception {
        String path = "/api/v1/admin/graduation-requirements/" + policy + "/impact";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        when(service.getImpact(auth, policy)).thenThrow(new AdminAccessDeniedException());
        mvc.perform(get(path).with(jwt().jwt(t -> t.subject(auth.toString())))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
        doThrow(new GraduationRequirementNotFoundException()).when(service).getImpact(auth, policy);
        mvc.perform(get(path).with(jwt().jwt(t -> t.subject(auth.toString())))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("GRADUATION_REQUIREMENT_NOT_FOUND"));
    }
}
