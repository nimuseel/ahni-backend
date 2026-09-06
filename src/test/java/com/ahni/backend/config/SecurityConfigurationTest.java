package com.ahni.backend.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigurationTest.ProtectedController.class)
@Import({
    SecurityConfiguration.class,
    SecurityConfigurationTest.ProtectedController.class
})
@Tag("integration")
class SecurityConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증되지_않은_사용자는_보호된_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/security-test"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증된_사용자는_보호된_API에_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/v1/security-test").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/v1/security-test")
        String protectedEndpoint() {
            return "ok";
        }
    }

}