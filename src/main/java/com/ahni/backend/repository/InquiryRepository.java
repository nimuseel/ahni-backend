package com.ahni.backend.repository;

import com.ahni.backend.entity.Student;
import com.ahni.backend.entity.Inquiry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @EntityGraph(attributePaths = {"answeredByAdmin"})
    List<Inquiry> findAllByStudentOrderByCreatedAtDesc(Student student);

    @EntityGraph(attributePaths = {"answeredByAdmin"})
    Optional<Inquiry> findByEntityIdAndStudent(UUID entityId, Student student);
}
