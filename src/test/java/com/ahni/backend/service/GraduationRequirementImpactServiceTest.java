package com.ahni.backend.service;

import com.ahni.backend.entity.*;
import com.ahni.backend.exception.*;
import com.ahni.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraduationRequirementImpactServiceTest {
    @Mock AdminRepository admins;
    @Mock GraduationRequirementRepository policies;
    @Mock StudentMajorRepository majors;
    @InjectMocks GraduationRequirementImpactService service;

    @Test
    void impact_is_restricted_to_administrators_and_matching_policy() {
        UUID auth = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.getImpact(auth, missing)).isInstanceOf(AdminAccessDeniedException.class);
        verifyNoInteractions(policies, majors);
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(auth)).thenReturn(true);
        assertThatThrownBy(() -> service.getImpact(auth, missing)).isInstanceOf(GraduationRequirementNotFoundException.class);
    }
    @Test
    void returns_the_exact_policy_count_without_mutating_it() {
        UUID auth = UUID.randomUUID();
        Department department = new Department("소프트웨어융합공학과");
        var policy = new GraduationRequirement(department, 2024, MajorType.DOUBLE_MAJOR, new BigDecimal("130.0"), new BigDecimal("36.0"), new BigDecimal("30.0"), "공식 안내", null);
        when(admins.existsByAuthUserIdAndDeletedAtIsNull(auth)).thenReturn(true);
        when(policies.findByEntityId(policy.getEntityId())).thenReturn(Optional.of(policy));
        when(majors.countAffectedStudents(department, 2024, MajorType.DOUBLE_MAJOR)).thenReturn(7L);
        var impact = service.getImpact(auth, policy.getEntityId());
        assertThat(impact.affectedStudentCount()).isEqualTo(7);
        assertThat(impact.requirementEntityId()).isEqualTo(policy.getEntityId());
        verify(policies, never()).save(any());
    }
}
