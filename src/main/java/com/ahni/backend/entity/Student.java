package com.ahni.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();

    @Column(nullable = false, unique = true, updatable = false)
    private UUID authUserId;

    @Column(length = 100)
    private String nickname;

    @Column(length = 320, nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer admissionYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EnrollmentStatus enrollmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    protected Student() { }

    public Student(
        UUID authUserId,
        String email,
        int admissionYear,
        EnrollmentStatus enrollmentStatus,
        String nickname
    ) {
        this.authUserId = authUserId;
        this.email = email;
        this.admissionYear = admissionYear;
        this.enrollmentStatus = enrollmentStatus;
        this.nickname = nickname;
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
