package com.ahni.backend.service;

import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.InquiryStatus;
import com.ahni.backend.dto.InquiryCreateRequest;
import com.ahni.backend.dto.InquiryResponse;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.Inquiry;
import com.ahni.backend.exception.InquiryNotFoundException;
import com.ahni.backend.exception.InquiryUpdateConflictException;
import com.ahni.backend.exception.InvalidInquiryException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.InquiryRepository;
import com.ahni.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {
    @Mock
    private StudentRepository students;

    @Mock
    private InquiryRepository inquiries;

    private InquiryService inquiryService;
    private UUID authUserId;
    private Student student;

    @BeforeEach
    void setUp() {
        inquiryService = new InquiryService(students, inquiries);
        authUserId = UUID.randomUUID();
        student = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
    }

    @Test
    void 인증된_학생이_문의를_등록한다() {
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.saveAndFlush(any(Inquiry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InquiryResponse response = inquiryService.create(
            authUserId,
            new InquiryCreateRequest(" 성적 등록 문의 ", " 내용입니다. ")
        );

        assertThat(response.title()).isEqualTo("성적 등록 문의");
        assertThat(response.content()).isEqualTo("내용입니다.");
        assertThat(response.status()).isEqualTo(InquiryStatus.SUBMITTED);
        assertThat(response.answer()).isNull();
    }

    @Test
    void 잘못된_문의는_안전한_실패로_변환한다() {
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> inquiryService.create(
            authUserId,
            new InquiryCreateRequest(" ", "내용입니다.")
        )).isInstanceOf(InvalidInquiryException.class);
    }

    @Test
    void 자신의_문의_목록을_최신순_저장소_쿼리로_조회한다() {
        Inquiry inquiry = new Inquiry(student, "문의", "내용");
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findAllByStudentAndDeletedAtIsNullOrderByCreatedAtDesc(student))
            .thenReturn(List.of(inquiry));

        List<InquiryResponse> result = inquiryService.getMine(authUserId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("문의");
        verify(inquiries).findAllByStudentAndDeletedAtIsNullOrderByCreatedAtDesc(student);
    }

    @Test
    void 자신의_문의_상세를_조회한다() {
        Inquiry inquiry = new Inquiry(student, "문의", "내용");
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findByEntityIdAndStudentAndDeletedAtIsNull(inquiry.getEntityId(), student))
            .thenReturn(Optional.of(inquiry));

        InquiryResponse result = inquiryService.getMine(
            authUserId,
            inquiry.getEntityId()
        );

        assertThat(result.entityId()).isEqualTo(inquiry.getEntityId());
        assertThat(result.content()).isEqualTo("내용");
    }

    @Test
    void 자신의_문의가_아니면_없는_문의로_처리한다() {
        UUID inquiryEntityId = UUID.randomUUID();
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findByEntityIdAndStudentAndDeletedAtIsNull(inquiryEntityId, student))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getMine(authUserId, inquiryEntityId))
            .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void 답변_전_문의는_수정할_수_있다() {
        Inquiry inquiry = new Inquiry(student, "이전 문의", "이전 내용");
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findByEntityIdAndStudentAndDeletedAtIsNull(inquiry.getEntityId(), student))
            .thenReturn(Optional.of(inquiry));

        InquiryResponse result = inquiryService.update(
            authUserId,
            inquiry.getEntityId(),
            new InquiryCreateRequest(" 수정 문의 ", " 수정 내용 ")
        );

        assertThat(result.title()).isEqualTo("수정 문의");
        assertThat(result.content()).isEqualTo("수정 내용");
    }

    @Test
    void 답변된_문의는_수정할_수_없다() {
        Inquiry inquiry = new Inquiry(student, "문의", "내용");
        ReflectionTestUtils.setField(inquiry, "status", InquiryStatus.ANSWERED);
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findByEntityIdAndStudentAndDeletedAtIsNull(inquiry.getEntityId(), student))
            .thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.update(
            authUserId,
            inquiry.getEntityId(),
            new InquiryCreateRequest("수정 문의", "수정 내용")
        )).isInstanceOf(InquiryUpdateConflictException.class);
    }

    @Test
    void 문의를_학생_화면에서_삭제한다() {
        Inquiry inquiry = new Inquiry(student, "문의", "내용");
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(inquiries.findByEntityIdAndStudentAndDeletedAtIsNull(inquiry.getEntityId(), student))
            .thenReturn(Optional.of(inquiry));

        inquiryService.delete(authUserId, inquiry.getEntityId());

        assertThat(inquiry.getDeletedAt()).isNotNull();
    }

    @Test
    void 학생_프로필이_없으면_문의_저장소를_호출하지_않는다() {
        when(students.findByAuthUserId(authUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getMine(authUserId))
            .isInstanceOf(StudentNotFoundException.class);
        verifyNoInteractions(inquiries);
    }
}
