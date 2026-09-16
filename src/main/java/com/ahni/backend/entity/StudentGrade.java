package com.ahni.backend.entity;

import com.ahni.backend.domain.AcademicTerm;
import com.ahni.backend.domain.GradeCode;
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
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "student_grade")
@Getter
public class StudentGrade {
    private static final int MIN_ACADEMIC_YEAR = 2000;
    private static final BigDecimal MIN_CREDIT = BigDecimal.ZERO;
    private static final BigDecimal MAX_CREDIT = new BigDecimal("30.0");
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private int academicYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AcademicTerm term;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GradeCode gradeCode;

    @Column(precision = 3, scale = 2)
    private BigDecimal gradePoint;

    @Column(precision = 4, scale = 1, nullable = false)
    private BigDecimal credit;

    @Column(name = "is_rpl", nullable = false)
    private boolean rpl;

    @Column(name = "is_retake", nullable = false)
    private boolean retake;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StudentGrade() { }

    public StudentGrade(
        Student student,
        Course course,
        int academicYear,
        AcademicTerm term,
        GradeCode gradeCode,
        BigDecimal credit,
        boolean rpl,
        boolean retake
    ) {
        validateRequiredFields(student, course, term);
        validateAcademicYear(academicYear);
        validateCredit(credit);
        validateGrade(gradeCode, rpl);

        this.student = student;
        this.course = course;
        this.academicYear = academicYear;
        this.term = term;
        this.gradeCode = gradeCode;
        this.gradePoint = rpl ? null : gradeCode.gradePoint();
        this.credit = credit;
        this.rpl = rpl;
        this.retake = retake;
    }

    private static void validateRequiredFields(
        Student student,
        Course course,
        AcademicTerm term
    ) {
        if (student == null || course == null || term == null) {
            throw new IllegalArgumentException("학생, 과목, 학기는 필수입니다.");
        }
    }

    private static void validateAcademicYear(int academicYear) {
        int currentYear = Year.now(SERVICE_ZONE_ID).getValue();
        if (academicYear < MIN_ACADEMIC_YEAR || academicYear > currentYear) {
            throw new IllegalArgumentException("수강연도가 올바르지 않습니다.");
        }
    }

    private static void validateCredit(BigDecimal credit) {
        if (credit == null
            || credit.compareTo(MIN_CREDIT) <= 0
            || credit.compareTo(MAX_CREDIT) > 0
            || credit.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException(
                "학점은 0보다 크고 30 이하여야 하며 소수점 한 자리까지만 입력할 수 있습니다."
            );
        }
    }

    private static void validateGrade(GradeCode gradeCode, boolean rpl) {
        if (rpl && gradeCode != null) {
            throw new IllegalArgumentException("RPL 성적에는 등급을 입력할 수 없습니다.");
        }
        if (!rpl && gradeCode == null) {
            throw new IllegalArgumentException("일반 성적에는 등급이 필수입니다.");
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
