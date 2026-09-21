package com.ahni.backend.service;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.dto.GradeCategorySummaryResponse;
import com.ahni.backend.dto.GradeSummaryResponse;
import com.ahni.backend.entity.StudentGrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class GradeSummaryCalculator {
    private GradeSummaryCalculator() { }

    static GradeSummaryResponse calculate(List<StudentGrade> grades) {
        Set<UUID> replacedGradeIds = grades.stream()
            .map(StudentGrade::getReplacedGrade)
            .filter(replacedGrade -> replacedGrade != null)
            .map(StudentGrade::getEntityId)
            .collect(Collectors.toSet());
        List<StudentGrade> activeGrades = grades.stream()
            .filter(grade -> !replacedGradeIds.contains(grade.getEntityId()))
            .toList();

        SummaryValues total = summarize(activeGrades);
        List<GradeCategorySummaryResponse> categories = Arrays.stream(
            CourseCategory.values()
        ).map(category -> {
            SummaryValues values = summarize(activeGrades.stream()
                .filter(grade -> grade.getCourse().getCategory() == category)
                .toList());
            return new GradeCategorySummaryResponse(
                category,
                values.gpa(),
                values.completedCredits(),
                values.gpaCredits()
            );
        }).toList();

        return new GradeSummaryResponse(
            total.gpa(),
            total.completedCredits(),
            total.gpaCredits(),
            categories
        );
    }

    private static SummaryValues summarize(List<StudentGrade> grades) {
        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal completedCredits = BigDecimal.ZERO;
        BigDecimal gpaCredits = BigDecimal.ZERO;

        for (StudentGrade grade : grades) {
            BigDecimal credit = grade.getCredit();
            if (grade.isRpl()) {
                completedCredits = completedCredits.add(credit);
                continue;
            }

            switch (grade.getGradeCode()) {
                case P -> completedCredits = completedCredits.add(credit);
                case NP -> { }
                case F -> gpaCredits = gpaCredits.add(credit);
                default -> {
                    completedCredits = completedCredits.add(credit);
                    gpaCredits = gpaCredits.add(credit);
                    numerator = numerator.add(
                        credit.multiply(grade.getGradePoint())
                    );
                }
            }
        }

        return new SummaryValues(
            gpa(numerator, gpaCredits),
            completedCredits.setScale(1),
            gpaCredits.setScale(1)
        );
    }

    private static BigDecimal gpa(
        BigDecimal numerator,
        BigDecimal gpaCredits
    ) {
        if (gpaCredits.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return numerator.divide(gpaCredits, 2, RoundingMode.HALF_UP);
    }

    private record SummaryValues(
        BigDecimal gpa,
        BigDecimal completedCredits,
        BigDecimal gpaCredits
    ) { }
}
