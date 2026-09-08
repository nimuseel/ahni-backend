package com.ahni.backend.repository;

import com.ahni.backend.entity.StudentMajor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentMajorRepository extends JpaRepository<StudentMajor, Long> {
}
