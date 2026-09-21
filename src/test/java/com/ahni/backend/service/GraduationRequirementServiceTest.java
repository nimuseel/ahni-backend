package com.ahni.backend.service;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.EnrollmentStatus;
import com.ahni.backend.domain.RequiredCourseCategory;
import com.ahni.backend.dto.GraduationRequirementResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.RequiredCourse;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentMajor;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.repository.GraduationRequirementRepository;
import com.ahni.backend.repository.RequiredCourseRepository;
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
class GraduationRequirementServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMajorRepository studentMajorRepository;

    @Mock
    private GraduationRequirementRepository graduationRequirementRepository;

    @Mock
    private RequiredCourseRepository requiredCourseRepository;

    @InjectMocks
    private GraduationRequirementService graduationRequirementService;

    @Test
    void 학생의_입학연도와_활성_전공에_맞는_졸업요건과_필수과목을_조회한다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        Student student = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "아니"
        );
        StudentMajor major = new StudentMajor(student, department, MajorType.PRIMARY);
        GraduationRequirement requirement = new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            BigDecimal.ZERO
        );
        Course course = new Course(
            department,
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
        RequiredCourse requiredCourse = new RequiredCourse(
            requirement,
            course,
            RequiredCourseCategory.MAJOR_FOUNDATION
        );
        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(major));
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            department,
            2024,
            MajorType.PRIMARY
        )).thenReturn(Optional.of(requirement));
        when(requiredCourseRepository.findAllActiveByGraduationRequirement(requirement))
            .thenReturn(List.of(requiredCourse));

        List<GraduationRequirementResponse> responses = graduationRequirementService
            .getRequirements(authUserId);

        assertThat(responses).hasSize(1);
        GraduationRequirementResponse response = responses.getFirst();
        assertThat(response.entityId()).isEqualTo(requirement.getEntityId());
        assertThat(response.admissionYear()).isEqualTo(2024);
        assertThat(response.majorType()).isEqualTo("PRIMARY");
        assertThat(response.department().entityId()).isEqualTo(department.getEntityId());
        assertThat(response.minTotalCredit()).isEqualByComparingTo("130.0");
        assertThat(response.requiredCourses()).hasSize(1);
        assertThat(response.requiredCourses().getFirst().category())
            .isEqualTo(RequiredCourseCategory.MAJOR_FOUNDATION);
        assertThat(response.requiredCourses().getFirst().course().code())
            .isEqualTo("CSE101");
    }

    @Test
    void 활성_전공에_맞는_졸업요건이_없으면_조회할_수_없다() {
        UUID authUserId = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        Student student = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "아니"
        );
        StudentMajor major = new StudentMajor(student, department, MajorType.PRIMARY);
        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(major));
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            department,
            2024,
            MajorType.PRIMARY
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> graduationRequirementService.getRequirements(authUserId))
            .isInstanceOf(GraduationRequirementNotFoundException.class);
    }

    @Test
    void 다중전공_졸업요건은_주전공_복수전공_부전공_순으로_반환한다() {
        UUID authUserId = UUID.randomUUID();
        Student student = new Student(
            authUserId,
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "아니"
        );
        Department primaryDepartment = new Department("소프트웨어융합공학과");
        Department minorDepartment = new Department("금융투자학과");
        StudentMajor primaryMajor = new StudentMajor(
            student,
            primaryDepartment,
            MajorType.PRIMARY
        );
        StudentMajor minor = new StudentMajor(
            student,
            minorDepartment,
            MajorType.MINOR
        );
        GraduationRequirement primaryRequirement = requirement(
            primaryDepartment,
            MajorType.PRIMARY
        );
        GraduationRequirement minorRequirement = requirement(
            minorDepartment,
            MajorType.MINOR
        );
        when(studentRepository.findByAuthUserId(authUserId))
            .thenReturn(Optional.of(student));
        when(studentMajorRepository.findAllByStudentAndDeletedAtIsNull(student))
            .thenReturn(List.of(minor, primaryMajor));
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            primaryDepartment,
            2024,
            MajorType.PRIMARY
        )).thenReturn(Optional.of(primaryRequirement));
        when(graduationRequirementRepository.findByDepartmentAndAdmissionYearAndMajorType(
            minorDepartment,
            2024,
            MajorType.MINOR
        )).thenReturn(Optional.of(minorRequirement));
        when(requiredCourseRepository.findAllActiveByGraduationRequirement(primaryRequirement))
            .thenReturn(List.of());
        when(requiredCourseRepository.findAllActiveByGraduationRequirement(minorRequirement))
            .thenReturn(List.of());

        List<GraduationRequirementResponse> responses = graduationRequirementService
            .getRequirements(authUserId);

        assertThat(responses)
            .extracting(GraduationRequirementResponse::majorType)
            .containsExactly("PRIMARY", "MINOR");
    }

    private GraduationRequirement requirement(
        Department department,
        MajorType majorType
    ) {
        return new GraduationRequirement(
            department,
            2024,
            majorType,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            BigDecimal.ZERO
        );
    }
}
