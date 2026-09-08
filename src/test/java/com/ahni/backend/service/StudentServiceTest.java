package com.ahni.backend.service;

import com.ahni.backend.dto.StudentProfileRegistrationRequest;
import com.ahni.backend.dto.StudentProfileResponse;
import com.ahni.backend.entity.*;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.InvalidEnrollmentStatusException;
import com.ahni.backend.exception.StudentAlreadyRegisteredException;
import com.ahni.backend.repository.DepartmentRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import com.ahni.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StudentMajorRepository studentMajorRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void 학생_프로필과_주전공을_등록한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");

        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(department.getEntityId(), 2024, EnrollmentStatus.ENROLLED, "인하");

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(department.getEntityId())).thenReturn(Optional.of(department));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileResponse response = studentService.registerProfile(authUserId, "student@inha.edu", request);

        ArgumentCaptor<StudentMajor> majorCaptor = ArgumentCaptor.forClass(StudentMajor.class);

        verify(studentMajorRepository).save(majorCaptor.capture());

        StudentMajor savedMajor = majorCaptor.getValue();

        assertThat(response.email()).isEqualTo("student@inha.edu");
        assertThat(response.nickname()).isEqualTo("인하");
        assertThat(response.admissionYear()).isEqualTo(2024);
        assertThat(response.primaryDepartment().entityId()).isEqualTo(department.getEntityId());

        assertThat(savedMajor.getMajorType()).isEqualTo(MajorType.PRIMARY);
        assertThat(savedMajor.getDepartment()).isEqualTo(department);
        assertThat(savedMajor.getStudent().getAuthUserId()).isEqualTo(authUserId);
    }

    @Test
    void 이미_프로필이_존재하면_등록할_수_없다() {
        UUID authUserId = UUID.randomUUID();

        Student existingStudent = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );

        StudentProfileRegistrationRequest request =
            new StudentProfileRegistrationRequest(
                UUID.randomUUID(),
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existingStudent));

        assertThatThrownBy(() -> studentService.registerProfile(authUserId, "student@inha.edu", request))
            .isInstanceOf(StudentAlreadyRegisteredException.class);

        verify(studentRepository, never()).save(any());
        verifyNoInteractions(departmentRepository, studentMajorRepository);
    }

    @Test
    void 존재하지_않는_학과로는_등록할_수_없다() {
        UUID authUserId = UUID.randomUUID();
        UUID departmentEntityId = UUID.randomUUID();

        StudentProfileRegistrationRequest request =
            new StudentProfileRegistrationRequest(
                departmentEntityId,
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(departmentEntityId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.registerProfile(
            authUserId,
            "student@ina.edu",
            request
        )).isInstanceOf(DepartmentNotFoundException.class);

        verify(studentRepository, never()).save(any());
        verifyNoInteractions(studentMajorRepository);
    }

    @Test
    void 가입할_때_졸업_상태는_선택할_수_없다() {
        UUID authUserId = UUID.randomUUID();

        StudentProfileRegistrationRequest request =
            new StudentProfileRegistrationRequest(
                UUID.randomUUID(),
                2024,
                EnrollmentStatus.GRADUATED,
                "인하"
            );

        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> studentService.registerProfile(
                authUserId,
                "student@inha.edu",
                request
            )
        ).isInstanceOf(InvalidEnrollmentStatusException.class);

        verifyNoInteractions(departmentRepository);
        verify(studentRepository, never()).save(any());
        verifyNoInteractions(studentMajorRepository);
    }

    @Test
    void 공백_닉네임은_null로_저장한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");

        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(department.getEntityId(), 2024, EnrollmentStatus.ENROLLED, "  ");

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(department.getEntityId())).thenReturn(Optional.of(department));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.registerProfile(authUserId, "student@inha.edu", request);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        verify(studentRepository).save(studentCaptor.capture());

        assertThat(studentCaptor.getValue().getNickname()).isNull();
    }
}