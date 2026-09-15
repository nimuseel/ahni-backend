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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    public CourseService(
        CourseRepository courseRepository,
        DepartmentRepository departmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<CourseResponse> getCourses(UUID departmentEntityId, String categoryValue) {
        CourseCategory category = parseCategory(categoryValue);
        Department department = findDepartment(departmentEntityId);
        List<Course> courses = findCourses(department, category);

        return courses.stream()
            .map(this::toResponse)
            .toList();
    }

    private CourseCategory parseCategory(String categoryValue) {
        if (categoryValue == null) {
            return null;
        }

        try {
            return CourseCategory.valueOf(categoryValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidCourseCategoryException();
        }
    }

    private Department findDepartment(UUID departmentEntityId) {
        if (departmentEntityId == null) {
            return null;
        }

        return departmentRepository.findByEntityIdAndDeletedAtIsNull(departmentEntityId)
            .orElseThrow(DepartmentNotFoundException::new);
    }

    private List<Course> findCourses(Department department, CourseCategory category) {
        if (department != null && category != null) {
            return courseRepository
                .findAllByActiveTrueAndDepartmentAndCategoryOrderByCodeAsc(
                    department,
                    category
                );
        }
        if (department != null) {
            return courseRepository.findAllByActiveTrueAndDepartmentOrderByCodeAsc(department);
        }
        if (category != null) {
            return courseRepository.findAllByActiveTrueAndCategoryOrderByCodeAsc(category);
        }
        return courseRepository.findAllByActiveTrueOrderByCodeAsc();
    }

    private CourseResponse toResponse(Course course) {
        Department department = course.getDepartment();
        DepartmentResponse departmentResponse = department == null
            ? null
            : new DepartmentResponse(department.getEntityId(), department.getName());

        return new CourseResponse(
            course.getEntityId(),
            course.getCode(),
            course.getName(),
            course.getCredit(),
            course.getCategory(),
            departmentResponse
        );
    }
}
