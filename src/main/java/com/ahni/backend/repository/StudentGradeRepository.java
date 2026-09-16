package com.ahni.backend.repository;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.StudentGrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    boolean existsByStudentAndCourseAndAcademicYearAndTerm(
        Student student,
        Course course,
        int academicYear,
        AcademicTerm term
    );

    @EntityGraph(attributePaths = {"course", "course.department"})
    List<StudentGrade> findAllByStudent(Student student);
}
