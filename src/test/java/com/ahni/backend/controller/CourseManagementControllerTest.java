package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.AdminCourseResponse;
import com.ahni.backend.exception.*;
import com.ahni.backend.service.CourseManagementService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseManagementController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class CourseManagementControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CourseManagementService service;
    @MockitoBean JwtDecoder decoder;
    final UUID authId = UUID.randomUUID();
    final UUID courseId = UUID.randomUUID();
    private static final String INPUT = "{\"code\":\"GEN101\",\"name\":\"글쓰기\",\"credit\":3.0,\"category\":\"GENERAL_EDUCATION\",\"departmentEntityId\":null}";

    @Test
    void mutations_and_listing_require_a_session() throws Exception {
        mvc.perform(get("/api/v1/admin/courses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/courses").contentType("application/json").content(INPUT)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/courses/" + courseId).contentType("application/json").content(INPUT)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/courses/" + courseId + "/deactivate")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void admin_can_create_update_and_deactivate_the_same_course() throws Exception {
        var active = new AdminCourseResponse(courseId, "GEN101", "글쓰기", new BigDecimal("3.0"), CourseCategory.GENERAL_EDUCATION, null, true);
        when(service.create(eq(authId), any())).thenReturn(active);
        when(service.update(eq(authId), eq(courseId), any())).thenReturn(active);
        when(service.deactivate(authId, courseId)).thenReturn(new AdminCourseResponse(courseId, "GEN101", "글쓰기", new BigDecimal("3.0"), CourseCategory.GENERAL_EDUCATION, null, false));
        mvc.perform(post("/api/v1/admin/courses").with(jwt().jwt(t -> t.subject(authId.toString()))).contentType("application/json").content(INPUT)).andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("GEN101"));
        mvc.perform(put("/api/v1/admin/courses/" + courseId).with(jwt().jwt(t -> t.subject(authId.toString()))).contentType("application/json").content(INPUT)).andExpect(status().isOk()).andExpect(jsonPath("$.entityId").value(courseId.toString()));
        mvc.perform(post("/api/v1/admin/courses/" + courseId + "/deactivate").with(jwt().jwt(t -> t.subject(authId.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void invalid_inputs_and_non_admins_have_stable_errors() throws Exception {
        mvc.perform(post("/api/v1/admin/courses").with(jwt()).contentType("application/json").content("{\"code\":\"\",\"credit\":30.12}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        when(service.findAll(authId, null, null, null)).thenThrow(new AdminAccessDeniedException());
        mvc.perform(get("/api/v1/admin/courses").with(jwt().jwt(t -> t.subject(authId.toString())))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
        when(service.create(eq(authId), any())).thenThrow(new CourseAlreadyExistsException());
        mvc.perform(post("/api/v1/admin/courses").with(jwt().jwt(t -> t.subject(authId.toString()))).contentType("application/json").content(INPUT)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURSE_ALREADY_EXISTS"));
    }
}
