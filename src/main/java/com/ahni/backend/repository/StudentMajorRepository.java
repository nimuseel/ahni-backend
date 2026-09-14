package com.ahni.backend.repository;

import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentMajor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMajorRepository extends JpaRepository<StudentMajor, Long> {
    List<StudentMajor> findAllByStudentAndDeletedAtIsNull(Student student);
}
