package com.ahni.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant deletedAt;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID entityId = UUID.randomUUID();

    protected Department() {}

    public Department(String name) {
        if (name == null || name.length() > 100 || name.isBlank() || name.isEmpty()) {
            throw new IllegalArgumentException("학과명은 100자 이내로 입력해 주세요.");
        }

        this.name = name;
    }

    public void rename(String name) {
        if (name == null || name.length() > 100 || name.isBlank() || name.isEmpty()) {
            throw new IllegalArgumentException("학과명은 100자 이내로 입력해 주세요.");
        }

        this.name = name;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
