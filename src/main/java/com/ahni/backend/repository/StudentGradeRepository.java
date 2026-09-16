package com.ahni.backend.repository;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    boolean existsByStudentAndCourseAndAcademicYearAndTerm(
        Student student,
        Course course,
        int academicYear,
        AcademicTerm term
    );

    boolean existsByStudentAndCourseAndAcademicYearAndTermAndIdNot(
        Student student,
        Course course,
        int academicYear,
        AcademicTerm term,
        Long id
    );

    Optional<StudentGrade> findByEntityIdAndStudent(UUID entityId, Student student);

    @EntityGraph(attributePaths = {"course", "course.department"})
    List<StudentGrade> findAllByStudent(Student student);
}
