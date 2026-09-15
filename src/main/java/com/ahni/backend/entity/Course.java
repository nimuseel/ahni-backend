package com.ahni.backend.entity;

import com.ahni.backend.domain.CourseCategory;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "course")
@Getter
public class Course {
    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 200;
    private static final BigDecimal MIN_CREDIT = BigDecimal.ZERO;
    private static final BigDecimal MAX_CREDIT = new BigDecimal("30.0");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = MAX_CODE_LENGTH, nullable = false, unique = true)
    private String code;

    @Column(length = MAX_NAME_LENGTH, nullable = false)
    private String name;

    @Column(precision = 4, scale = 1, nullable = false)
    private BigDecimal credit;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private CourseCategory category;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Course() { }

    public Course(
        Department department,
        String code,
        String name,
        BigDecimal credit,
        CourseCategory category
    ) {
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.credit = validateCredit(credit);
        this.category = validateCategory(category);
        validateDepartment(department, category);
        this.department = department;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("과목 코드는 필수입니다.");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("과목 코드는 30자를 초과할 수 없습니다.");
        }
        return normalizedCode;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("과목명은 필수입니다.");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("과목명은 200자를 초과할 수 없습니다.");
        }
        return normalizedName;
    }

    private static BigDecimal validateCredit(BigDecimal credit) {
        if (credit == null) {
            throw new IllegalArgumentException("학점은 필수입니다.");
        }
        if (credit.compareTo(MIN_CREDIT) < 0 || credit.compareTo(MAX_CREDIT) > 0) {
            throw new IllegalArgumentException("학점은 0부터 30 사이여야 합니다.");
        }
        if (credit.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("학점은 소수점 한 자리까지만 입력할 수 있습니다.");
        }
        return credit;
    }

    private static CourseCategory validateCategory(CourseCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("과목 분류는 필수입니다.");
        }
        return category;
    }

    private static void validateDepartment(Department department, CourseCategory category) {
        if (category == CourseCategory.MAJOR && department == null) {
            throw new IllegalArgumentException("전공 과목은 학과가 필수입니다.");
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
