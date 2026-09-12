package com.ahni.backend.repository;

import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentMajor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentMajorRepository extends JpaRepository<StudentMajor, Long> {
    Optional<StudentMajor> findByStudentAndMajorTypeAndDeletedAtIsNull(Student student, MajorType majorType);
}
