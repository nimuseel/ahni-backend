package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.dto.GraduationRequirementImpactResponse;
import com.ahni.backend.exception.AdminAccessDeniedException;
import com.ahni.backend.exception.GraduationRequirementNotFoundException;
import com.ahni.backend.repository.AdminRepository;
import com.ahni.backend.repository.GraduationRequirementRepository;
import com.ahni.backend.repository.StudentMajorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GraduationRequirementImpactService {
    private final AdminRepository admins;
    private final GraduationRequirementRepository policies;
    private final StudentMajorRepository majors;

    public GraduationRequirementImpactService(
        AdminRepository admins,
        GraduationRequirementRepository policies,
        StudentMajorRepository majors
    ) {
        this.admins = admins;
        this.policies = policies;
        this.majors = majors;
    }

    public GraduationRequirementImpactResponse getImpact(UUID authUserId, UUID policyEntityId) {
        if (!admins.existsByAuthUserIdAndDeletedAtIsNull(authUserId)) {
            throw new AdminAccessDeniedException();
        }
        var policy = policies.findByEntityId(policyEntityId)
            .orElseThrow(GraduationRequirementNotFoundException::new);
        var department = policy.getDepartment();
        return new GraduationRequirementImpactResponse(
            policy.getEntityId(),
            policy.getAdmissionYear(),
            policy.getMajorType().name(),
            new DepartmentResponse(department.getEntityId(), department.getName()),
            majors.countAffectedStudents(department, policy.getAdmissionYear(), policy.getMajorType())
        );
    }
}
