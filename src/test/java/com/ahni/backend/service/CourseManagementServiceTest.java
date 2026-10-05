package com.ahni.backend.service;

import com.ahni.backend.domain.*;
import com.ahni.backend.dto.CourseManagementRequest;
import com.ahni.backend.entity.*;
import com.ahni.backend.exception.*;
import com.ahni.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseManagementServiceTest {
    @Mock AdminRepository admins;
    @Mock CourseRepository courses;
    @Mock DepartmentRepository departments;
    @Mock RequiredCourseRepository assignments;
    @InjectMocks CourseManagementService service;
    final UUID admin = UUID.randomUUID();
    final Department department = new Department("소프트웨어융합공학과");

    @Test
    void only_administrators_can_list_or_mutate_courses() {
        assertThatThrownBy(() -> service.findAll(admin, null, null, null)).isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> service.create(admin, request("CSE101"))).isInstanceOf(AdminAccessDeniedException.class);
        assertThatThrownBy(() -> service.deactivate(admin, UUID.randomUUID())).isInstanceOf(AdminAccessDeniedException.class);
        verifyNoInteractions(courses);
    }

    @Test
    void creation_normalizes_code_and_preserves_decimal_credit() {
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(admin)).thenReturn(true);
        when(departments.findByEntityIdAndDeletedAtIsNull(department.getEntityId())).thenReturn(Optional.of(department));
        when(courses.saveAndFlush(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(admin, request(" cse101 "));
        assertThat(result.code()).isEqualTo("CSE101");
        assertThat(result.credit()).isEqualByComparingTo("3.0");
        assertThat(result.active()).isTrue();
    }

    @Test
    void duplicate_normalized_codes_are_conflicts() {
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(admin)).thenReturn(true);
        when(departments.findByEntityIdAndDeletedAtIsNull(department.getEntityId())).thenReturn(Optional.of(department));
        when(courses.existsByCodeAndEntityIdNot(eq("CSE101"), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(admin, request("cse101"))).isInstanceOf(CourseAlreadyExistsException.class);
        verify(courses, never()).saveAndFlush(any());
    }

    @Test
    void deactivation_retains_identity_and_grade_snapshots() {
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(admin)).thenReturn(true);
        var course = new Course(department, "CSE101", "프로그래밍", new BigDecimal("3.0"), CourseCategory.MAJOR);
        var student = new Student(UUID.randomUUID(), "student@inha.edu", 2024, EnrollmentStatus.ENROLLED, null);
        var grade = new StudentGrade(student, course, 2024, AcademicTerm.FIRST, GradeCode.A_PLUS, new BigDecimal("3.0"), false, null);
        when(courses.findByEntityId(course.getEntityId())).thenReturn(Optional.of(course));
        service.deactivate(admin, course.getEntityId());
        assertThat(course.isActive()).isFalse();
        assertThat(grade.getCredit()).isEqualByComparingTo("3.0");
        assertThat(grade.getCourse().getEntityId()).isEqualTo(course.getEntityId());
        verify(courses, never()).delete(any());
    }

    @Test
    void assigned_required_course_cannot_be_changed_to_an_incompatible_category() {
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(admin)).thenReturn(true);
        var course = new Course(department, "CSE101", "프로그래밍", new BigDecimal("3.0"), CourseCategory.MAJOR);
        var policy = new GraduationRequirement(department, 2024, MajorType.PRIMARY, new BigDecimal("130.0"), new BigDecimal("60.0"), new BigDecimal("30.0"), "학과 기준", null);
        when(courses.findByEntityId(course.getEntityId())).thenReturn(Optional.of(course));
        when(assignments.findAllByCourseAndDeletedAtIsNull(course)).thenReturn(List.of(new RequiredCourse(policy, course, RequiredCourseCategory.MAJOR_REQUIRED)));
        var input = new CourseManagementRequest(null, "CSE101", "프로그래밍", new BigDecimal("3.0"), CourseCategory.ELECTIVE);
        assertThatThrownBy(() -> service.update(admin, course.getEntityId(), input)).isInstanceOf(CourseAssignmentConflictException.class);
        assertThat(course.getCategory()).isEqualTo(CourseCategory.MAJOR);
    }

    private CourseManagementRequest request(String code) {
        return new CourseManagementRequest(department.getEntityId(), code, "프로그래밍", new BigDecimal("3.0"), CourseCategory.MAJOR);
    }
}
