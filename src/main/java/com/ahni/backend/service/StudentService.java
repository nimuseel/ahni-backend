package com.ahni.backend.service;

import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.StudentProfileRegistrationRequest;
import com.ahni.backend.dto.StudentProfileResponse;
import com.ahni.backend.entity.*;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.InvalidEnrollmentStatusException;
import com.ahni.backend.exception.StudentAlreadyRegisteredException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.DepartmentRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import com.ahni.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentMajorRepository studentMajorRepository;

    public StudentService(StudentRepository studentRepository, DepartmentRepository departmentRepository, StudentMajorRepository studentMajorRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.studentMajorRepository = studentMajorRepository;
    }

    public StudentProfileResponse getProfile(UUID authUserId) {
        Student student = studentRepository.findByAuthUserId(authUserId)
            .orElseThrow(StudentNotFoundException::new);
        StudentMajor primaryMajor = studentMajorRepository
            .findByStudentAndMajorTypeAndDeletedAtIsNull(student, MajorType.PRIMARY)
            .orElseThrow(() -> new IllegalStateException("학생의 주전공을 찾을 수 없습니다."));

        return toResponse(student, primaryMajor.getDepartment());
    }

    @Transactional
    public StudentProfileResponse registerProfile(UUID authUserId, String email, StudentProfileRegistrationRequest request) {
        if (studentRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new StudentAlreadyRegisteredException();
        }

        EnrollmentStatus enrollmentStatus = request.enrollmentStatus();

        if (enrollmentStatus != EnrollmentStatus.ENROLLED && enrollmentStatus != EnrollmentStatus.LEAVE) {
            throw new InvalidEnrollmentStatusException();
        }

        String nickname = getNickname(request.nickname());

        Student student = new Student(authUserId, email, request.admissionYear(), enrollmentStatus, nickname);
        Department department = departmentRepository.findByEntityId(request.primaryDepartmentEntityId()).filter(dp -> dp.getDeletedAt() == null).orElseThrow(DepartmentNotFoundException::new);

        Student savedStudent = studentRepository.save(student);

        StudentMajor primaryMajor = new StudentMajor(savedStudent, department, MajorType.PRIMARY);
        studentMajorRepository.save(primaryMajor);

        return toResponse(student, department);

    }

    private static StudentProfileResponse toResponse(Student student, Department department) {
        return new StudentProfileResponse(
            student.getEntityId(),
            student.getEmail(),
            student.getNickname(),
            new DepartmentResponse(
                department.getEntityId(),
                department.getName()
            ),
            student.getAdmissionYear(),
            student.getEnrollmentStatus(),
            student.getAccountStatus()
        );
    }

    private static String getNickname(String nickname) {
        return nickname == null || nickname.isBlank() ? null : nickname.trim();
    }
}
