package com.smartverse.churchlitebackend.service.permissions;

import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.dtos.PermissionGroupDTO;
import com.smartverse.churchlitebackend_gen.services.PermissionGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PermissionGroupCrudService extends PermissionGroupService {
    private final SubscriptionService subscriptionService;

    public PermissionGroupCrudService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public PermissionGroupDTO save(PermissionGroupDTO dto) {
        if (dto.isActive()) {
            subscriptionService.requireAvailable(SubscriptionResource.ACTIVE_PERMISSION_GROUP);
        }
        return super.save(dto);
    }

    @Override
    @Transactional
    public PermissionGroupDTO update(PermissionGroupDTO dto, UUID id) {
        boolean activating = dto.isActive()
                && repository.findById(id).map(item -> !item.isActive()).orElse(true);
        if (activating) {
            subscriptionService.requireAvailable(SubscriptionResource.ACTIVE_PERMISSION_GROUP);
        }
        return super.update(dto, id);
    }
}
