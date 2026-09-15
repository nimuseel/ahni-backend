package com.ahni.backend.service;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.exception.DepartmentNotFoundException;
import com.ahni.backend.exception.InvalidCourseCategoryException;
import com.ahni.backend.repository.CourseRepository;
import com.ahni.backend.repository.DepartmentRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void 필터가_없으면_전체_활성_과목을_반환한다() {
        Department department = new Department("소프트웨어융합공학과");
        Course course = majorCourse(department);
        when(courseRepository.findAllByActiveTrueOrderByCodeAsc())
            .thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getCourses(null, null);

        assertThat(result).containsExactly(expectedMajorResponse(course, department));
    }

    @Test
    void 학과로_활성_과목을_필터링한다() {
        Department department = new Department("소프트웨어융합공학과");
        Course course = majorCourse(department);
        UUID departmentEntityId = department.getEntityId();
        when(departmentRepository.findByEntityIdAndDeletedAtIsNull(departmentEntityId))
            .thenReturn(Optional.of(department));
        when(courseRepository.findAllByActiveTrueAndDepartmentOrderByCodeAsc(department))
            .thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getCourses(departmentEntityId, null);

        assertThat(result).containsExactly(expectedMajorResponse(course, department));
    }

    @Test
    void 분류로_활성_과목을_필터링하고_소문자도_정규화한다() {
        Department department = new Department("소프트웨어융합공학과");
        Course course = majorCourse(department);
        when(courseRepository.findAllByActiveTrueAndCategoryOrderByCodeAsc(CourseCategory.MAJOR))
            .thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getCourses(null, " major ");

        assertThat(result).containsExactly(expectedMajorResponse(course, department));
    }

    @Test
    void 학과와_분류를_함께_적용한다() {
        Department department = new Department("소프트웨어융합공학과");
        Course course = majorCourse(department);
        UUID departmentEntityId = department.getEntityId();
        when(departmentRepository.findByEntityIdAndDeletedAtIsNull(departmentEntityId))
            .thenReturn(Optional.of(department));
        when(courseRepository.findAllByActiveTrueAndDepartmentAndCategoryOrderByCodeAsc(
            department,
            CourseCategory.MAJOR
        )).thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getCourses(departmentEntityId, "MAJOR");

        assertThat(result).containsExactly(expectedMajorResponse(course, department));
    }

    @Test
    void 유효하지_않은_과목_분류는_조회할_수_없다() {
        assertThatThrownBy(() -> courseService.getCourses(null, "REQUIRED"))
            .isInstanceOf(InvalidCourseCategoryException.class);

        verifyNoInteractions(courseRepository, departmentRepository);
    }

    @Test
    void 존재하지_않거나_삭제된_학과로는_조회할_수_없다() {
        UUID departmentEntityId = UUID.randomUUID();
        when(departmentRepository.findByEntityIdAndDeletedAtIsNull(departmentEntityId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourses(departmentEntityId, null))
            .isInstanceOf(DepartmentNotFoundException.class);

        verifyNoInteractions(courseRepository);
    }

    @Test
    void 학과가_없는_과목은_학과_정보를_null로_반환한다() {
        Course course = new Course(
            null,
            "GE101",
            "대학 글쓰기",
            new BigDecimal("2.0"),
            CourseCategory.GENERAL_EDUCATION
        );
        when(courseRepository.findAllByActiveTrueOrderByCodeAsc())
            .thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getCourses(null, null);

        assertThat(result).containsExactly(new CourseResponse(
            course.getEntityId(),
            "GE101",
            "대학 글쓰기",
            new BigDecimal("2.0"),
            CourseCategory.GENERAL_EDUCATION,
            null
        ));
    }

    @Test
    void 조회된_과목이_없으면_빈_목록을_반환한다() {
        when(courseRepository.findAllByActiveTrueOrderByCodeAsc())
            .thenReturn(List.of());

        List<CourseResponse> result = courseService.getCourses(null, null);

        assertThat(result).isEmpty();
        verify(courseRepository).findAllByActiveTrueOrderByCodeAsc();
    }

    private Course majorCourse(Department department) {
        return new Course(
            department,
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR
        );
    }

    private CourseResponse expectedMajorResponse(Course course, Department department) {
        return new CourseResponse(
            course.getEntityId(),
            "CSE101",
            "프로그래밍 기초",
            new BigDecimal("3.0"),
            CourseCategory.MAJOR,
            new DepartmentResponse(department.getEntityId(), department.getName())
        );
    }
}
