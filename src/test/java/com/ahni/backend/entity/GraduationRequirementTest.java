package com.ahni.backend.entity;

import com.ahni.backend.entity.MajorType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraduationRequirementTest {
    private final Department department = new Department("소프트웨어융합공학과");

    @Test
    void 입학연도와_전공유형별_졸업요건을_생성한다() {
        GraduationRequirement requirement = new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("130.0"),
            new BigDecimal("60.0"),
            new BigDecimal("30.0")
        );

        assertThat(requirement.getDepartment()).isSameAs(department);
        assertThat(requirement.getAdmissionYear()).isEqualTo(2024);
        assertThat(requirement.getMajorType()).isEqualTo(MajorType.PRIMARY);
        assertThat(requirement.getMinTotalCredit()).isEqualByComparingTo("130.0");
        assertThat(requirement.getMinDepartmentCredit()).isEqualByComparingTo("60.0");
        assertThat(requirement.getMinGeneralCredit()).isEqualByComparingTo("30.0");
    }

    @Test
    void 졸업요건의_기준값은_음수일_수_없다() {
        assertThatThrownBy(() -> new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("-0.1"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 졸업요건의_학점은_소수점_한자리까지만_허용한다() {
        assertThatThrownBy(() -> new GraduationRequirement(
            department,
            2024,
            MajorType.PRIMARY,
            new BigDecimal("130.01"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 졸업요건의_필수값을_검증한다() {
        assertThatThrownBy(() -> new GraduationRequirement(
            null,
            2024,
            MajorType.PRIMARY,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GraduationRequirement(
            department,
            1999,
            MajorType.PRIMARY,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GraduationRequirement(
            department,
            2024,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
