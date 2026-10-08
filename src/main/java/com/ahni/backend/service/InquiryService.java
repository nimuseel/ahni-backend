package com.ahni.backend.service;

import com.ahni.backend.dto.InquiryCreateRequest;
import com.ahni.backend.dto.InquiryResponse;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.Inquiry;
import com.ahni.backend.exception.InquiryNotFoundException;
import com.ahni.backend.exception.InvalidInquiryException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.InquiryRepository;
import com.ahni.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InquiryService {
    private final StudentRepository students;
    private final InquiryRepository inquiries;

    public InquiryService(
        StudentRepository students,
        InquiryRepository inquiries
    ) {
        this.students = students;
        this.inquiries = inquiries;
    }

    @Transactional
    public InquiryResponse create(UUID authUserId, InquiryCreateRequest request) {
        Student student = findStudent(authUserId);
        try {
            return toResponse(inquiries.saveAndFlush(
                new Inquiry(student, request.title(), request.content())
            ));
        } catch (IllegalArgumentException exception) {
            throw new InvalidInquiryException(exception.getMessage());
        }
    }

    public List<InquiryResponse> getMine(UUID authUserId) {
        Student student = findStudent(authUserId);
        return inquiries.findAllByStudentOrderByCreatedAtDesc(student).stream()
            .map(InquiryService::toResponse)
            .toList();
    }

    public InquiryResponse getMine(UUID authUserId, UUID inquiryEntityId) {
        Student student = findStudent(authUserId);
        return inquiries.findByEntityIdAndStudent(inquiryEntityId, student)
            .map(InquiryService::toResponse)
            .orElseThrow(InquiryNotFoundException::new);
    }

    private Student findStudent(UUID authUserId) {
        return students.findByAuthUserId(authUserId)
            .orElseThrow(StudentNotFoundException::new);
    }

    private static InquiryResponse toResponse(Inquiry inquiry) {
        return new InquiryResponse(
            inquiry.getEntityId(),
            inquiry.getTitle(),
            inquiry.getContent(),
            inquiry.getStatus(),
            inquiry.getAnswer(),
            inquiry.getAnsweredAt(),
            inquiry.getCreatedAt(),
            inquiry.getUpdatedAt()
        );
    }
}
