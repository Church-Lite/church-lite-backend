package com.smartverse.churchlitebackend.service.permissions;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PermissionGroupBusinessService {
    private final EntityManager entityManager;

    public PermissionGroupBusinessService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isDenied(UUID userId, String resource, String permission) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT count(*)
                FROM permission_group_denial d
                JOIN permission_group g
                    ON g.id = d.permission_group
                    AND g.active = true
                JOIN permission_group_member m
                    ON m.permission_group = g.id
                WHERE m.user_id = ?
                  AND d.resource = ?
                  AND d.permission = ?
                """)
                .setParameter(1, userId)
                .setParameter(2, resource)
                .setParameter(3, permission)
                .getSingleResult();

        return count != null && count.longValue() > 0;
    }
}
