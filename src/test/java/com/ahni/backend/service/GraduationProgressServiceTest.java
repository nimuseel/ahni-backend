package com.ahni.backend.service;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.dto.GraduationProgressResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import com.ahni.backend.entity.StudentMajor;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.repository.GraduationRequirementRepository;
import com.ahni.backend.repository.StudentGradeRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import com.ahni.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraduationProgressServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMajorRepository studentMajorRepository;

    @Mock
    private GraduationRequirementRepository graduationRequirementRepository;

    @Mock
    private StudentGradeRepository gradeRepository;

    @InjectMocks
    private GraduationProgressService graduationProgressService;

    @Test
    void 학생의_활성_전공별_학점_충족도를_계산한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        Student student = student(authUserId);
        StudentMajor major = new StudentMajor(student, department, MajorType.PRIMARY);
        GraduationRequirement requirement = requirement(department);
        Course course = new Course(
            department,
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
        StudentGrade grade = new StudentGrade(
            student,
            course,
            2025,
            AcademicTerm.SECOND,
            GradeCode.A_PLUS,
            new BigDecimal("3.0"),
            false,
            null
        );
        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(major));
        when(gradeRepository.findAllByStudent(student)).thenReturn(List.of(grade));
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            department,
            2024,
            MajorType.PRIMARY
        )).thenReturn(Optional.of(requirement));

        List<GraduationProgressResponse> responses = graduationProgressService
            .getProgress(authUserId);

        assertThat(responses).hasSize(1);
        GraduationProgressResponse response = responses.getFirst();
        assertThat(response.requirementEntityId()).isEqualTo(requirement.getEntityId());
        assertThat(response.majorType()).isEqualTo("PRIMARY");
        assertThat(response.department().entityId()).isEqualTo(department.getEntityId());
        assertThat(response.credits().total().completed()).isEqualByComparingTo("3.0");
        assertThat(response.credits().department().completed())
            .isEqualByComparingTo("3.0");
        assertThat(response.credits().general().completed()).isEqualByComparingTo("0.0");
    }

    @Test
    void 활성_전공에_맞는_졸업요건이_없으면_충족도를_계산할_수_없다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        Student student = student(authUserId);
        StudentMajor major = new StudentMajor(student, department, MajorType.PRIMARY);
        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(major));
        when(gradeRepository.findAllByStudent(student)).thenReturn(List.of());
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            department,
            2024,
            MajorType.PRIMARY
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> graduationProgressService.getProgress(authUserId))
            .isInstanceOf(GraduationRequirementNotFoundException.class);
    }

    private Student student(UUID authUserId) {
        return new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
    }

    private GraduationRequirement requirement(Department department) {
        return new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            new BigDecimal("30.0")
        );
    }
}
