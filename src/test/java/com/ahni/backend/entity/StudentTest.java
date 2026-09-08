package com.ahni.backend.entity;

import com.ahni.backend.domain.AccountStatus;
import com.ahni.backend.domain.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void 현재_연도보다_큰_입학연도로는_학생을_생성할_수_없다() {
        int nextYear = Year.now(ZoneId.of("Asia/Seoul")).plusYears(1).getValue();

        assertThatThrownBy(() -> new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            nextYear,
            EnrollmentStatus.ENROLLED,
            "인하"
        ))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 최소_입학연도보다_작으면_학생을_생성할_수_없다() {
        assertThatThrownBy(() -> new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            1999,
            EnrollmentStatus.ENROLLED,
            "인하"
        ))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
