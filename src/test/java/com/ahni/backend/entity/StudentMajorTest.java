package com.ahni.backend.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMajorTest {

    @Test
    void 학생과_학과를_주전공으로_연결할_수_있다() {
        Student student = new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
        Department department = new Department("소프트웨어융합공학과");

        StudentMajor studentMajor = new StudentMajor(student, department, MajorType.PRIMARY);

        assertThat(studentMajor.getStudent()).isEqualTo(student);
        assertThat(studentMajor.getDepartment()).isEqualTo(department);
        assertThat(studentMajor.getMajorType()).isEqualTo(MajorType.PRIMARY);
    }
}
