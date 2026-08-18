package com.smartverse.churchlitebackend.repository.memberportal;

import com.smartverse.churchlitebackend_gen.entities.PersonMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberDashboardRepository extends JpaRepository<PersonMemberEntity, UUID> {
    Optional<PersonMemberEntity> findByAccessUserHash(UUID accessUserHash);
}
