package com.ahni.backend.repository;

import com.ahni.backend.entity.GraduationRequirement;
import com.ahni.backend.entity.RequiredCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RequiredCourseRepository extends JpaRepository<RequiredCourse, Long> {
    @Query("""
        select requiredCourse
        from RequiredCourse requiredCourse
        join fetch requiredCourse.course
        where requiredCourse.graduationRequirement = :requirement
          and requiredCourse.deletedAt is null
        order by requiredCourse.course.code asc
        """)
    List<RequiredCourse> findAllActiveByGraduationRequirement(
        GraduationRequirement requirement
    );
}
