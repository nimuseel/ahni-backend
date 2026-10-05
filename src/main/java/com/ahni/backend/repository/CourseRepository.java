package com.ahni.backend.repository;

import com.ahni.backend.domain.CourseCategory;
import com.ahni.backend.entity.Course;
import com.ahni.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByEntityId(UUID entityId);
    boolean existsByCodeAndEntityIdNot(String code, UUID entityId);

    @org.springframework.data.jpa.repository.Query("""
        select c from Course c left join fetch c.department d
        where (:departmentEntityId is null or d.entityId = :departmentEntityId)
          and (:category is null or c.category = :category)
          and (:active is null or c.active = :active)
        order by c.code asc
        """)
    List<Course> findAllForAdmin(UUID departmentEntityId, CourseCategory category, Boolean active);
    Optional<Course> findByEntityIdAndActiveTrue(UUID entityId);

    List<Course> findAllByEntityIdInAndActiveTrue(List<UUID> entityIds);

    List<Course> findAllByActiveTrueOrderByCodeAsc();

    List<Course> findAllByActiveTrueAndDepartmentOrderByCodeAsc(Department department);

    List<Course> findAllByActiveTrueAndCategoryOrderByCodeAsc(CourseCategory category);

    List<Course> findAllByActiveTrueAndDepartmentAndCategoryOrderByCodeAsc(
        Department department,
        CourseCategory category
    );
}
