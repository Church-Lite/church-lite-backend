package com.smartverse.churchlitebackend.handlers.userconfiguration;

import com.smartverse.churchlitebackend.services.userconfiguration.UserConfigurationService;
import com.smartverse.churchlitebackend_gen.UserConfigurationDTO;
import com.smartverse.churchlitebackend_gen.UserConfigurationHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserConfigurationHandlerImpl extends UserConfigurationHandler {

    private final UserConfigurationService userConfigurationService;

    public UserConfigurationHandlerImpl(UserConfigurationService userConfigurationService) {
        this.userConfigurationService = userConfigurationService;
    }

    @Override
    @Transactional
    public UserConfigurationDTO update(UserConfigurationDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        entityManager.flush();
        userConfigurationService.updateMaster(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.findById(id).ifPresent(entity -> {
            userConfigurationService.deleteMaster(entity.getHash());
            repository.delete(entity);
        });
    }
}
