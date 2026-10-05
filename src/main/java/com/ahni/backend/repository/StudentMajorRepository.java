package com.ahni.backend.repository;

import com.ahni.backend.entity.Department;
import com.ahni.backend.entity.MajorType;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentMajorRepository extends JpaRepository<StudentMajor, Long> {
    @Query("""
        select count(distinct major.student.id) from StudentMajor major
        where major.department = :department and major.majorType = :majorType
          and major.student.admissionYear = :admissionYear
          and major.deletedAt is null and major.student.deletedAt is null
        """)
    long countAffectedStudents(Department department, Integer admissionYear, MajorType majorType);

    List<StudentMajor> findAllByStudentAndDeletedAtIsNull(Student student);
}
