package com.ahni.backend.service;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.CreditProgressResponse;
import com.ahni.backend.dto.GraduationCreditProgressResponse;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.StudentGrade;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class GraduationProgressCalculator {
    private GraduationProgressCalculator() { }

    static GraduationCreditProgressResponse calculate(
        GraduationRequirement requirement,
        List<StudentGrade> grades
    ) {
        BigDecimal totalCompleted = GradeSummaryCalculator.calculate(grades)
            .completedCredits();
        UUID departmentEntityId = requirement.getDepartment().getEntityId();
        BigDecimal departmentCompleted = completedCredits(grades.stream()
            .filter(grade -> grade.getCourse().getCategory() == CourseCategory.MAJOR)
            .filter(grade -> grade.getCourse().getDepartment() != null)
            .filter(grade -> departmentEntityId.equals(
                grade.getCourse().getDepartment().getEntityId()
            ))
            .toList());
        BigDecimal generalCompleted = completedCredits(grades.stream()
            .filter(grade -> grade.getCourse().getCategory()
                == CourseCategory.GENERAL_EDUCATION)
            .toList());

        return new GraduationCreditProgressResponse(
            progress(requirement.getMinTotalCredit(), totalCompleted),
            progress(requirement.getMinDepartmentCredit(), departmentCompleted),
            progress(requirement.getMinGeneralCredit(), generalCompleted)
        );
    }

    private static BigDecimal completedCredits(List<StudentGrade> grades) {
        return GradeSummaryCalculator.calculate(grades).completedCredits();
    }

    private static CreditProgressResponse progress(
        BigDecimal required,
        BigDecimal completed
    ) {
        BigDecimal remaining = required.subtract(completed).max(BigDecimal.ZERO)
            .setScale(1);
        return new CreditProgressResponse(
            required,
            completed,
            remaining,
            completed.compareTo(required) >= 0
        );
    }
}
