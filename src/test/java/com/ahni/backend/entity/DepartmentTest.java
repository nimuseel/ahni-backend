package com.ahni.backend.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {
    @Test
    void 학과_이름은_100자_이내로_생성_가능() {
        Department department = new Department("소프트웨어융합공학과");
        String length100Name = "가".repeat(100);
        Department department2 = new Department(length100Name);

        assertThat(department.getName()).isEqualTo("소프트웨어융합공학과");
        assertThat(department2.getName()).isEqualTo(length100Name);
    }

    @Test
    void 학과_이름이_100자_초과하면_생성_불가() {
        String length101Name = "가".repeat(101);
        assertThatThrownBy(() -> new Department(length101Name)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 학과_이름_변경은_100자_이내로_가능() {
        Department department = new Department("소프트웨어융합공학과");
        department.rename("산업경영학과");

        assertThat(department.getName()).isEqualTo("산업경영학과");
    }

    @Test
    void 학과_이름_변경_시_100자_초과하면_변경_불가() {
        Department department = new Department("소프트웨어융합공학과");
        String length101Name = "가".repeat(101);

        assertThatThrownBy(() -> department.rename(length101Name)).isInstanceOf(IllegalArgumentException.class);
        assertThat(department.getName()).isEqualTo("소프트웨어융합공학과");
    }

    @Test
    void 학과_이름이_null이거나_빈_문자면_설정_불가() {
        assertThatThrownBy(() -> new Department(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Department("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Department(" ")).isInstanceOf(IllegalArgumentException.class);

        Department department = new Department("소프트웨어융합공학과");
        assertThatThrownBy(() -> department.rename("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> department.rename(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> department.rename(null)).isInstanceOf(IllegalArgumentException.class);
    }
}