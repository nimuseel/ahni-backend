package com.ahni.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_curriculum_department_year", columnNames = {"department_id", "curriculum_year"}))
@Getter
public class Curriculum {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;
    @Column(nullable = false, updatable = false)
    private int curriculumYear;
    @Column(nullable = false, length = 200)
    private String sourceTitle;
    @Column(length = 2048)
    private String sourceUrl;
    @Column(nullable = false)
    private boolean published;
    @Version @Column(nullable = false)
    private long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Curriculum() { }

    public Curriculum(Department department, int curriculumYear, String sourceTitle, String sourceUrl) {
        if (department == null || curriculumYear < 2000 || curriculumYear > 9999) {
            throw new IllegalArgumentException("학과와 2000~9999년 교과과정 연도가 필요합니다.");
        }
        this.department = department;
        this.curriculumYear = curriculumYear;
        updateSource(sourceTitle, sourceUrl);
    }

    public void updateSource(String title, String url) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("출처명은 200자 이내로 입력해 주세요.");
        }
        String normalizedUrl = url == null || url.isBlank() ? null : url.trim();
        if (normalizedUrl != null) {
            URI uri = URI.create(normalizedUrl);
            if (normalizedUrl.length() > 2048 || uri.getHost() == null
                || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("출처 URL은 2048자 이내의 HTTP(S) 주소여야 합니다.");
            }
        }
        this.sourceTitle = title.trim();
        this.sourceUrl = normalizedUrl;
        this.published = false;
        this.updatedAt = Instant.now();
    }

    public void setPublished(boolean published) { this.published = published; }
    @PrePersist void onCreate() { this.createdAt = this.updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
