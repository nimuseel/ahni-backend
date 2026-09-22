package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.MajorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GraduationRequirementRepository
    extends JpaRepository<GraduationRequirement, Long> {
    Optional<GraduationRequirement> findByDepartmentAndAdmissionYearAndMajorType(
        Department department,
        Integer admissionYear,
        MajorType majorType
    );

    Optional<GraduationRequirement> findByEntityId(UUID entityId);

    List<GraduationRequirement> findAllByDepartmentOrderByAdmissionYearDescMajorTypeAsc(
        Department department
    );

    @Query("""
        select requirement
        from GraduationRequirement requirement
        join fetch requirement.department department
        where (:departmentEntityId is null or department.entityId = :departmentEntityId)
          and (:admissionYear is null or requirement.admissionYear = :admissionYear)
          and (:majorType is null or requirement.majorType = :majorType)
        order by department.name asc,
                 requirement.admissionYear desc,
                 requirement.majorType asc
        """)
    List<GraduationRequirement> findAllForAdmin(
        @Param("departmentEntityId") UUID departmentEntityId,
        @Param("admissionYear") Integer admissionYear,
        @Param("majorType") MajorType majorType
    );
}
