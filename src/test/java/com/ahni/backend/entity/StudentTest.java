package com.ahni.backend.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTest {
    @Test
    void 학생_프로필을_생성하면_계정_상태는_ACTIVE이다() {
        Student student = new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );

        assertThat(student.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
