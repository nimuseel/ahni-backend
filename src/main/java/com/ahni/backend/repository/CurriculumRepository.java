package com.ahni.backend.repository;

import com.ahni.backend.entity.Curriculum;
import com.ahni.backend.entity.Department;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    Optional<Curriculum> findByEntityId(UUID entityId);
    boolean existsByDepartmentAndCurriculumYear(Department department, int curriculumYear);
    @EntityGraph(attributePaths = "department")
    List<Curriculum> findAllByOrderByDepartmentNameAscCurriculumYearDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curriculum c where c.entityId = :entityId")
    Optional<Curriculum> findForUpdate(@Param("entityId") UUID entityId);
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from Curriculum c where c.curriculumYear = :year and c.department.deletedAt is null order by c.id")
    List<Curriculum> findYearForValidation(@Param("year") int year);
}
