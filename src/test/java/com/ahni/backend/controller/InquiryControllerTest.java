package com.ahni.backend.controller;

import com.ahni.backend.config.SecurityConfiguration;
import com.ahni.backend.domain.InquiryStatus;
import com.ahni.backend.dto.InquiryCreateRequest;
import com.ahni.backend.dto.InquiryResponse;
import com.ahni.backend.exception.InquiryNotFoundException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.service.InquiryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
@Import(SecurityConfiguration.class)
@Tag("integration")
class InquiryControllerTest {
    private static final UUID AUTH_USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000010"
    );
    private static final UUID INQUIRY_ENTITY_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000701"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_학생이_문의를_등록한다() throws Exception {
        InquiryCreateRequest request = new InquiryCreateRequest(
            "성적 등록 문의",
            "2025년 과목이 성적 등록 화면에 보이지 않습니다."
        );
        when(inquiryService.create(AUTH_USER_ID, request))
            .thenReturn(response(InquiryStatus.SUBMITTED, null, null));

        mockMvc.perform(post("/api/v1/inquiries")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "성적 등록 문의",
                      "content": "2025년 과목이 성적 등록 화면에 보이지 않습니다."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entityId").value(INQUIRY_ENTITY_ID.toString()))
            .andExpect(jsonPath("$.title").value("성적 등록 문의"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.answer").doesNotExist());

        verify(inquiryService).create(AUTH_USER_ID, request);
    }

    @Test
    void 제목이나_내용이_비어_있으면_문의_등록을_거절한다() throws Exception {
        mockMvc.perform(post("/api/v1/inquiries")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "", "content": " "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(inquiryService);
    }

    @Test
    void 인증된_학생이_자신의_문의_목록을_조회한다() throws Exception {
        when(inquiryService.getMine(AUTH_USER_ID))
            .thenReturn(List.of(response(InquiryStatus.ANSWERED, "확인했습니다.", Instant.parse("2026-10-09T01:00:00Z"))));

        mockMvc.perform(get("/api/v1/inquiries")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].entityId").value(INQUIRY_ENTITY_ID.toString()))
            .andExpect(jsonPath("$[0].status").value("ANSWERED"))
            .andExpect(jsonPath("$[0].answer").value("확인했습니다."));

        verify(inquiryService).getMine(AUTH_USER_ID);
    }

    @Test
    void 문의가_없으면_빈_목록을_응답한다() throws Exception {
        when(inquiryService.getMine(AUTH_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inquiries")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void 인증된_학생이_자신의_문의_상세를_조회한다() throws Exception {
        when(inquiryService.getMine(AUTH_USER_ID, INQUIRY_ENTITY_ID))
            .thenReturn(response(InquiryStatus.SUBMITTED, null, null));

        mockMvc.perform(get("/api/v1/inquiries/" + INQUIRY_ENTITY_ID)
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entityId").value(INQUIRY_ENTITY_ID.toString()))
            .andExpect(jsonPath("$.content").value("2025년 과목이 성적 등록 화면에 보이지 않습니다."));

        verify(inquiryService).getMine(AUTH_USER_ID, INQUIRY_ENTITY_ID);
    }

    @Test
    void 다른_학생의_문의처럼_찾을_수_없으면_404를_응답한다() throws Exception {
        when(inquiryService.getMine(AUTH_USER_ID, INQUIRY_ENTITY_ID))
            .thenThrow(new InquiryNotFoundException());

        mockMvc.perform(get("/api/v1/inquiries/" + INQUIRY_ENTITY_ID)
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    void 학생_프로필이_없으면_404를_응답한다() throws Exception {
        when(inquiryService.getMine(AUTH_USER_ID))
            .thenThrow(new StudentNotFoundException());

        mockMvc.perform(get("/api/v1/inquiries")
                .with(jwt().jwt(token -> token.subject(AUTH_USER_ID.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void 인증되지_않은_사용자는_문의를_등록하거나_조회할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "문의", "content": "내용"}
                    """))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/inquiries"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(inquiryService);
    }

    private static InquiryResponse response(
        InquiryStatus status,
        String answer,
        Instant answeredAt
    ) {
        return new InquiryResponse(
            INQUIRY_ENTITY_ID,
            "성적 등록 문의",
            "2025년 과목이 성적 등록 화면에 보이지 않습니다.",
            status,
            answer,
            answeredAt,
            Instant.parse("2026-10-09T00:00:00Z"),
            Instant.parse("2026-10-09T00:00:00Z")
        );
    }
}
