package com.ahni.backend.service;

import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.dto.StudentProfileRegistrationRequest;
import com.ahni.backend.dto.StudentProfileResponse;
import com.ahni.backend.entity.*;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.DuplicateMajorDepartmentException;
import com.ahni.backend.exception.InvalidEnrollmentStatusException;
import com.ahni.backend.exception.StudentAlreadyRegisteredException;
import com.ahni.backend.exception.StudentEmailAlreadyRegisteredException;
import com.ahni.backend.exception.StudentNotFoundException;
import com.ahni.backend.repository.DepartmentRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import com.ahni.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    void 인증된_학생의_프로필을_조회한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        Student student = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
        StudentMajor primaryMajor = new StudentMajor(student, department, MajorType.PRIMARY);

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(primaryMajor));

        StudentProfileResponse response = studentService.getProfile(authUserId);

        assertThat(response.studentEntityId()).isEqualTo(student.getEntityId());
        assertThat(response.email()).isEqualTo("student@inha.edu");
        assertThat(response.nickname()).isEqualTo("인하");
        assertThat(response.primaryDepartment().entityId()).isEqualTo(department.getEntityId());
        assertThat(response.primaryDepartment().name()).isEqualTo("소프트웨어융합공학과");
        assertThat(response.doubleMajorDepartment()).isNull();
        assertThat(response.minorDepartment()).isNull();
        assertThat(response.admissionYear()).isEqualTo(2024);
        assertThat(response.enrollmentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(response.accountStatus()).isEqualTo(student.getAccountStatus());
    }

    @Test
    void 등록되지_않은_학생의_프로필은_조회할_수_없다() {
        UUID authUserId = UUID.randomUUID();

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getProfile(authUserId))
            .isInstanceOf(StudentNotFoundException.class);

        verifyNoInteractions(studentMajorRepository);
    }

    @Test
    void 학생_프로필과_주전공을_등록한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");

        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
            department.getEntityId(),
            null,
            null,
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(department.getEntityId())).thenReturn(Optional.of(department));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileResponse response = studentService.registerProfile(authUserId, "student@inha.edu", request);

        assertThat(response.email()).isEqualTo("student@inha.edu");
        assertThat(response.nickname()).isEqualTo("인하");
        assertThat(response.admissionYear()).isEqualTo(2024);
        assertThat(response.primaryDepartment().entityId()).isEqualTo(department.getEntityId());

        verify(studentMajorRepository).saveAll(argThat(majors -> {
            List<StudentMajor> savedMajors = (List<StudentMajor>) majors;
            return savedMajors.size() == 1
                && savedMajors.getFirst().getMajorType() == MajorType.PRIMARY
                && savedMajors.getFirst().getDepartment().equals(department)
                && savedMajors.getFirst().getStudent().getAuthUserId().equals(authUserId);
        }));
    }

    @Test
    void 학생_프로필과_주전공_복수전공_부전공을_등록한다() {
        UUID authUserId = UUID.randomUUID();
        Department primary = new Department("소프트웨어융합공학과");
        Department doubleMajor = new Department("금융투자학과");
        Department minor = new Department("산업경영학과");
        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
            primary.getEntityId(),
            doubleMajor.getEntityId(),
            minor.getEntityId(),
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(primary.getEntityId()))
            .thenReturn(Optional.of(primary));
        when(departmentRepository.findByEntityId(doubleMajor.getEntityId()))
            .thenReturn(Optional.of(doubleMajor));
        when(departmentRepository.findByEntityId(minor.getEntityId()))
            .thenReturn(Optional.of(minor));
        when(studentRepository.save(any(Student.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileResponse response = studentService.registerProfile(
            authUserId,
            "student@inha.edu",
            request
        );

        verify(studentMajorRepository).saveAll(argThat(majors -> {
            List<MajorType> types = ((List<StudentMajor>) majors).stream()
                .map(StudentMajor::getMajorType)
                .toList();
            return types.size() == 3
                && types.containsAll(List.of(
                    MajorType.PRIMARY,
                    MajorType.DOUBLE_MAJOR,
                    MajorType.MINOR
                ));
        }));
        assertThat(response.primaryDepartment().entityId())
            .isEqualTo(primary.getEntityId());
        assertThat(response.doubleMajorDepartment().entityId())
            .isEqualTo(doubleMajor.getEntityId());
        assertThat(response.minorDepartment().entityId())
            .isEqualTo(minor.getEntityId());
    }

    @Test
    void 같은_학과를_여러_전공으로_등록할_수_없다() {
        UUID authUserId = UUID.randomUUID();
        UUID departmentEntityId = UUID.randomUUID();
        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
            departmentEntityId,
            null,
            departmentEntityId,
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.registerProfile(
            authUserId,
            "student@inha.edu",
            request
        )).isInstanceOf(DuplicateMajorDepartmentException.class);

        verifyNoInteractions(departmentRepository, studentMajorRepository);
        verify(studentRepository, never()).save(any());
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
                null,
                null,
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
    void 같은_이메일로_등록된_학생이_있으면_등록할_수_없다() {
        UUID authUserId = UUID.randomUUID();
        String email = "student@inha.edu";
        StudentProfileRegistrationRequest request =
            new StudentProfileRegistrationRequest(
                UUID.randomUUID(),
                null,
                null,
                2024,
                EnrollmentStatus.ENROLLED,
                "인하"
            );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(studentRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

        assertThatThrownBy(() -> studentService.registerProfile(authUserId, email, request))
            .isInstanceOf(StudentEmailAlreadyRegisteredException.class);

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
                null,
                null,
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
                null,
                null,
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

        StudentProfileRegistrationRequest request = new StudentProfileRegistrationRequest(
            department.getEntityId(),
            null,
            null,
            2024,
            EnrollmentStatus.ENROLLED,
            "  "
        );

        when(studentRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(departmentRepository.findByEntityId(department.getEntityId())).thenReturn(Optional.of(department));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.registerProfile(authUserId, "student@inha.edu", request);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        verify(studentRepository).save(studentCaptor.capture());

        assertThat(studentCaptor.getValue().getNickname()).isNull();
    }
}
