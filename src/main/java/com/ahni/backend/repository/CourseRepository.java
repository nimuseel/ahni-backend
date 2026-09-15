package com.ahni.backend.repository;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByActiveTrueOrderByCodeAsc();

    List<Course> findAllByActiveTrueAndDepartmentOrderByCodeAsc(Department department);

    List<Course> findAllByActiveTrueAndCategoryOrderByCodeAsc(CourseCategory category);

    List<Course> findAllByActiveTrueAndDepartmentAndCategoryOrderByCodeAsc(
        Department department,
        CourseCategory category
    );
}
