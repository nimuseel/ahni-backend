package com.ahni.backend.service;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.domain.RequiredCourseCategory;
import com.ahni.backend.dto.AdminCourseResponse;
import com.ahni.backend.dto.CourseManagementRequest;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import com.ahni.backend.exception.*;
import com.ahni.backend.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CourseManagementService {
    private final AdminRepository admins;
    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final RequiredCourseRepository assignments;

    public CourseManagementService(AdminRepository admins, CourseRepository courses, DepartmentRepository departments, RequiredCourseRepository assignments) {
        this.admins = admins;
        this.courses = courses;
        this.departments = departments;
        this.assignments = assignments;
    }

    public List<AdminCourseResponse> findAll(UUID authUserId, UUID departmentEntityId, CourseCategory category, Boolean active) {
        ensureAdmin(authUserId);
        return courses.findAllForAdmin(departmentEntityId, category, active).stream().map(CourseManagementService::response).toList();
    }

    @Transactional
    public AdminCourseResponse create(UUID authUserId, CourseManagementRequest input) {
        ensureAdmin(authUserId);
        Course course = new Course(department(input.departmentEntityId()), input.code(), input.name(), input.credit(), input.category());
        ensureUnique(course.getCode(), course.getEntityId());
        return response(flush(course));
    }

    @Transactional
    public AdminCourseResponse update(UUID authUserId, UUID courseEntityId, CourseManagementRequest input) {
        ensureAdmin(authUserId);
        Course course = find(courseEntityId);
        Department department = department(input.departmentEntityId());
        Course validated = new Course(department, input.code(), input.name(), input.credit(), input.category());
        for (var assignment : assignments.findAllByCourseAndDeletedAtIsNull(course)) {
            CourseCategory expected = assignment.getCategory() == RequiredCourseCategory.GENERAL_REQUIRED ? CourseCategory.GENERAL_EDUCATION : CourseCategory.MAJOR;
            UUID previousDepartment = course.getDepartment() == null ? null : course.getDepartment().getEntityId();
            if (validated.getCategory() != expected || !Objects.equals(previousDepartment, input.departmentEntityId())) {
                throw new CourseAssignmentConflictException();
            }
        }
        ensureUnique(validated.getCode(), courseEntityId);
        course.update(department, validated.getCode(), validated.getName(), validated.getCredit(), validated.getCategory());
        return response(flush(course));
    }

    @Transactional
    public AdminCourseResponse deactivate(UUID authUserId, UUID courseEntityId) {
        ensureAdmin(authUserId);
        Course course = find(courseEntityId);
        course.deactivate();
        return response(course);
    }

    private void ensureAdmin(UUID id) {
        if (!admins.existsByAuthUserIdAndDeletedAtIsNull(id)) throw new AdminAccessDeniedException();
    }
    private Course find(UUID id) { return courses.findByEntityId(id).orElseThrow(CourseNotFoundException::new); }
    private Department department(UUID id) {
        return id == null ? null : departments.findByEntityIdAndDeletedAtIsNull(id).orElseThrow(DepartmentNotFoundException::new);
    }
    private void ensureUnique(String code, UUID excludedId) {
        if (courses.existsByCodeAndEntityIdNot(code, excludedId)) throw new CourseAlreadyExistsException();
    }
    private Course flush(Course course) {
        try { return courses.saveAndFlush(course); }
        catch (DataIntegrityViolationException exception) {
            String cause = exception.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uk_course_code")) throw new CourseAlreadyExistsException();
            throw exception;
        }
    }
    private static AdminCourseResponse response(Course course) {
        var department = course.getDepartment();
        return new AdminCourseResponse(course.getEntityId(), course.getCode(), course.getName(), course.getCredit(), course.getCategory(), department == null ? null : new DepartmentResponse(department.getEntityId(), department.getName()), course.isActive());
    }
}
