package com.smartverse.churchlitebackend.config.security.repository;

import com.smartverse.churchlitebackend.config.security.model.MemberPortalChurchLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberPortalChurchLinkRepository extends JpaRepository<MemberPortalChurchLinkEntity, UUID> {
    Optional<MemberPortalChurchLinkEntity> findByTenant(String tenant);
}
