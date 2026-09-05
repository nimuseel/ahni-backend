package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByEntityId(UUID entityId);
}
