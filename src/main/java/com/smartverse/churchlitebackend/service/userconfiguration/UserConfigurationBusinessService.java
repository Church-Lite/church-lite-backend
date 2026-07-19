package com.smartverse.churchlitebackend.service.userconfiguration;

import com.smartverse.churchlitebackend_gen.dtos.UserConfigurationDTO;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserConfigurationBusinessService
        extends com.smartverse.churchlitebackend_gen.services.UserConfigurationService {

    private final UserConfigurationService masterUserConfigurationService;
    private final EntityManager entityManager;

    public UserConfigurationBusinessService(
            UserConfigurationService masterUserConfigurationService,
            EntityManager entityManager) {
        this.masterUserConfigurationService = masterUserConfigurationService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public UserConfigurationDTO update(UserConfigurationDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        entityManager.flush();
        masterUserConfigurationService.updateMaster(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.findById(id).ifPresent(entity -> {
            masterUserConfigurationService.deleteMaster(entity.getHash());
            repository.delete(entity);
        });
    }
}
