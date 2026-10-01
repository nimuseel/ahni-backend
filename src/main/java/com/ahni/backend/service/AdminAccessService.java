package com.ahni.backend.service;

import com.ahni.backend.dto.AdminIdentityResponse;
import com.ahni.backend.entity.Admin;
import com.ahni.backend.exception.AdminAccessDeniedException;
import com.ahni.backend.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminAccessService {
    private final AdminRepository adminRepository;

    public AdminAccessService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public AdminIdentityResponse getCurrentAdmin(UUID authUserId) {
        Admin admin = adminRepository.findByAuthUserIdAndDeletedAtIsNull(authUserId)
            .orElseThrow(AdminAccessDeniedException::new);
        return new AdminIdentityResponse(
            admin.getEntityId(),
            admin.getName(),
            admin.getEmail()
        );
    }
}
