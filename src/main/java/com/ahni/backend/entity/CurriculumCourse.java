package com.ahni.backend.entity;

import com.ahni.backend.domain.CurriculumDivision;
import com.ahni.backend.domain.RecommendedTerm;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_curriculum_course", columnNames = {"curriculum_id", "course_id"}))
@Getter
public class CurriculumCourse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private CurriculumDivision division;
    private Integer recommendedYear;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private RecommendedTerm recommendedTerm;
    @Column(length = 30) private String areaCode;
    @Column(length = 100) private String areaName;
    @Column(length = 100) private String majorArea;
    @Column(columnDefinition = "text") private String note;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected CurriculumCourse() { }

    public CurriculumCourse(Curriculum curriculum, Course course, CurriculumDivision division,
        Integer recommendedYear, RecommendedTerm recommendedTerm, String areaCode,
        String areaName, String majorArea, String note) {
        if (curriculum == null || course == null || division == null) {
            throw new IllegalArgumentException("교과과정, 과목과 분류는 필수입니다.");
        }
        if (recommendedYear != null && (recommendedYear < 1 || recommendedYear > 6)) {
            throw new IllegalArgumentException("권장 학년은 1~6이어야 합니다.");
        }
        this.curriculum = curriculum;
        this.course = course;
        this.division = division;
        this.recommendedYear = recommendedYear;
        this.recommendedTerm = recommendedTerm;
        this.areaCode = normalize(areaCode, 30);
        this.areaName = normalize(areaName, 100);
        this.majorArea = normalize(majorArea, 100);
        this.note = normalize(note, 4000);
    }

    private static String normalize(String value, int maximum) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maximum) throw new IllegalArgumentException("입력값의 최대 길이를 초과했습니다.");
        return normalized;
    }
    @PrePersist void onCreate() { this.createdAt = this.updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
