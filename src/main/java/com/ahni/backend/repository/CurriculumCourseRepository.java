package com.ahni.backend.repository;

import com.ahni.backend.entity.Curriculum;
import com.ahni.backend.entity.CurriculumCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ahni.backend.entity.Course;

import java.util.List;
import java.util.UUID;

public interface CurriculumCourseRepository extends JpaRepository<CurriculumCourse, Long> {
    boolean existsByCourse(Course course);
    @EntityGraph(attributePaths = {"course", "course.department"})
    List<CurriculumCourse> findAllByCurriculumOrderByCourseCodeAsc(Curriculum curriculum);
    @EntityGraph(attributePaths = {"course", "course.department"})
    List<CurriculumCourse> findAllByCurriculumInOrderByCourseCodeAsc(List<Curriculum> curricula);

    @Query("""
        select distinct course from CurriculumCourse link
        join link.course course
        left join fetch course.department
        join link.curriculum curriculum
        where curriculum.published = true and curriculum.curriculumYear = :year
          and curriculum.department.deletedAt is null
          and (:departmentId is null or curriculum.department.entityId = :departmentId)
        order by course.code asc
        """)
    List<Course> findPublishedCourses(@Param("year") int year, @Param("departmentId") UUID departmentId);
}
