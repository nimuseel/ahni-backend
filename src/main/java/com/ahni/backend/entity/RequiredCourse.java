package com.ahni.backend.entity;

import com.ahni.backend.domain.RequiredCourseCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "required_course")
@Getter
public class RequiredCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "graduation_requirement_id", nullable = false)
    private GraduationRequirement graduationRequirement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private RequiredCourseCategory category;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant deletedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RequiredCourse() { }

    public RequiredCourse(
        GraduationRequirement graduationRequirement,
        Course course,
        RequiredCourseCategory category
    ) {
        if (graduationRequirement == null) {
            throw new IllegalArgumentException("졸업요건은 필수입니다.");
        }
        if (course == null) {
            throw new IllegalArgumentException("과목은 필수입니다.");
        }
        if (category == null) {
            throw new IllegalArgumentException("필수과목 분류는 필수입니다.");
        }

        this.graduationRequirement = graduationRequirement;
        this.course = course;
        this.category = category;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public void softDelete() {
        if (isActive()) {
            this.deletedAt = Instant.now();
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
