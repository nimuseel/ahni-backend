package com.ahni.backend.repository;

import com.ahni.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByAuthUserId(UUID authUserId);
}
