package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAllByDeletedAtIsNullOrderByNameAsc()
            .stream()
            .map(department -> new DepartmentResponse(
                department.getEntityId(),
                department.getName()
            ))
            .toList();
    }
}
