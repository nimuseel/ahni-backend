package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.StudentProfileRegistrationRequest;
import com.ahni.backend.dto.StudentProfileResponse;
import com.ahni.backend.entity.*;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.InvalidEnrollmentStatusException;
import com.ahni.backend.exception.StudentAlreadyRegisteredException;
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

    @Transactional
    public StudentProfileResponse registerProfile(UUID authUserId, String email, StudentProfileRegistrationRequest request) {
        if (studentRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new StudentAlreadyRegisteredException();
        }

        EnrollmentStatus status = request.enrollmentStatus();

        if (status != EnrollmentStatus.ENROLLED && status != EnrollmentStatus.LEAVE) {
            throw new InvalidEnrollmentStatusException();
        }

        String nickname = getNickname(request.nickname());

        if (nickname.isEmpty()) {
            nickname = null;
        }

        Student student = new Student(authUserId, email, request.admissionYear(), request.enrollmentStatus(), nickname);
        Department department = departmentRepository.findByEntityId(request.primaryDepartmentEntityId()).filter(dp -> dp.getDeletedAt() == null).orElseThrow(DepartmentNotFoundException::new);

        Student savedStudent = studentRepository.save(student);

        StudentMajor primaryMajor = new StudentMajor(savedStudent, department, MajorType.PRIMARY);
        studentMajorRepository.save(primaryMajor);

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
