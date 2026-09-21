package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GraduationRequirementRepository
    extends JpaRepository<GraduationRequirement, Long> {
    Optional<GraduationRequirement> findByDepartmentAndAdmissionYearAndMajorType(
        Department department,
        Integer admissionYear,
        MajorType majorType
    );

    List<GraduationRequirement> findAllByDepartmentOrderByAdmissionYearDescMajorTypeAsc(
        Department department
    );
}
