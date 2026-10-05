package com.ahni.backend.service;
import com.ahni.backend.domain.GradeCode;
import com.ahni.backend.dto.CourseResponse;
import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.RequiredCourseProgressResponse;
import com.ahni.backend.entity.RequiredCourse;
import com.ahni.backend.entity.StudentGrade;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
final class RequiredCourseProgressCalculator {
    private RequiredCourseProgressCalculator() { }
    static List<RequiredCourseProgressResponse> calculate(List<RequiredCourse> assignments, List<StudentGrade> grades) {
        Set<UUID> completedIds = GradeSummaryCalculator.effectiveAttempts(grades).stream()
            .filter(grade -> grade.isRpl() || (grade.getGradeCode() != GradeCode.F && grade.getGradeCode() != GradeCode.NP))
            .map(grade -> grade.getCourse().getEntityId()).collect(Collectors.toSet());
        return assignments.stream().map(assignment -> {
            var course = assignment.getCourse();
            var department = course.getDepartment();
            return new RequiredCourseProgressResponse(assignment.getEntityId(), assignment.getCategory(), new CourseResponse(course.getEntityId(), course.getCode(), course.getName(), course.getCredit(), course.getCategory(), department == null ? null : new DepartmentResponse(department.getEntityId(), department.getName())), completedIds.contains(course.getEntityId()));
        }).toList();
    }
}
