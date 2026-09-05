package com.ahni.backend.service;

import com.ahni.backend.dto.DepartmentResponse;
import com.ahni.backend.entity.Department;
import com.ahni.backend.repository.DepartmentRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository);
    }

    @Test
    void 학과_목록을_반환한다() {
        Department finance =
            new Department("금융투자학과");
        Department software =
            new Department("소프트웨어융합공학과");

        when(
            departmentRepository.findAllByDeletedAtIsNullOrderByNameAsc()
        ).thenReturn(List.of(finance, software));

        List<DepartmentResponse> departments = departmentService.getDepartments();

        assertThat(departments)
            .containsExactly(
                new DepartmentResponse(
                    finance.getEntityId(),
                    finance.getName()
                ),
                new DepartmentResponse(
                    software.getEntityId(),
                    software.getName())
            );
    }
}