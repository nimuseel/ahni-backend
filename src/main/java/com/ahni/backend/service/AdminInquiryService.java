package com.ahni.backend.service;

import com.ahni.backend.dto.AdminInquiryResponse;
import com.ahni.backend.dto.AdminInquiryStudentResponse;
import com.ahni.backend.dto.InquiryAnswerRequest;
import com.ahni.backend.entity.Admin;
import com.ahni.backend.entity.Inquiry;
import com.ahni.backend.entity.Student;
import com.ahni.backend.exception.InquiryNotFoundException;
import com.ahni.backend.exception.InvalidInquiryException;
import com.ahni.backend.repository.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminInquiryService {
    private final AdminAccessService access;
    private final InquiryRepository inquiries;

    public AdminInquiryService(
        AdminAccessService access,
        InquiryRepository inquiries
    ) {
        this.access = access;
        this.inquiries = inquiries;
    }

    public List<AdminInquiryResponse> findAll(UUID authUserId) {
        access.getAdmin(authUserId);
        return inquiries.findAllByOrderByCreatedAtDesc().stream()
            .map(AdminInquiryService::response)
            .toList();
    }

    @Transactional
    public AdminInquiryResponse get(UUID authUserId, UUID inquiryEntityId) {
        access.getAdmin(authUserId);
        Inquiry inquiry = find(inquiryEntityId);
        inquiry.markInReview();
        return response(inquiry);
    }

    @Transactional
    public AdminInquiryResponse answer(
        UUID authUserId,
        UUID inquiryEntityId,
        InquiryAnswerRequest request
    ) {
        Admin admin = access.getAdmin(authUserId);
        Inquiry inquiry = find(inquiryEntityId);
        if (inquiry.getDeletedAt() != null) {
            throw new InvalidInquiryException("삭제된 문의에는 답변할 수 없습니다.");
        }
        try {
            inquiry.answer(admin, request.answer());
            return response(inquiry);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInquiryException(exception.getMessage());
        }
    }

    private Inquiry find(UUID inquiryEntityId) {
        return inquiries.findByEntityId(inquiryEntityId)
            .orElseThrow(InquiryNotFoundException::new);
    }

    private static AdminInquiryResponse response(Inquiry inquiry) {
        Student student = inquiry.getStudent();
        Admin answeredByAdmin = inquiry.getAnsweredByAdmin();
        return new AdminInquiryResponse(
            inquiry.getEntityId(),
            new AdminInquiryStudentResponse(
                student.getEntityId(),
                student.getEmail(),
                student.getNickname()
            ),
            inquiry.getTitle(),
            inquiry.getContent(),
            inquiry.getStatus(),
            inquiry.getAnswer(),
            answeredByAdmin == null ? null : answeredByAdmin.getName(),
            inquiry.getAnsweredAt(),
            inquiry.getCreatedAt(),
            inquiry.getUpdatedAt(),
            inquiry.getDeletedAt()
        );
    }
}
