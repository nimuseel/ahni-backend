package com.ahni.backend.entity;

import com.ahni.backend.domain.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMajorTest {

    @Test
    void 학생과_학과를_주전공으로_연결할_수_있다() {
        Student student = createStudent();
        Department department = new Department("소프트웨어융합공학과");

        StudentMajor studentMajor = new StudentMajor(student, department, MajorType.PRIMARY);

        assertThat(studentMajor.getStudent()).isEqualTo(student);
        assertThat(studentMajor.getDepartment()).isEqualTo(department);
        assertThat(studentMajor.getMajorType()).isEqualTo(MajorType.PRIMARY);
    }

    @Test
    void 전공_관계를_소프트_삭제할_수_있다() {
        StudentMajor major = new StudentMajor(
            createStudent(),
            new Department("산업경영학과"),
            MajorType.MINOR
        );

        major.softDelete();

        assertThat(major.getDeletedAt()).isNotNull();
    }

    @Test
    void 같은_학과에_연결되어_있는지_확인할_수_있다() {
        Department department = new Department("소프트웨어융합공학과");
        StudentMajor major = new StudentMajor(createStudent(), department, MajorType.PRIMARY);

        assertThat(major.isAssignedTo(department)).isTrue();
    }

    private Student createStudent() {
        return new Student(
            UUID.randomUUID(),
            "student@inha.edu",
            2024,
            EnrollmentStatus.ENROLLED,
            "인하"
        );
    }
}
