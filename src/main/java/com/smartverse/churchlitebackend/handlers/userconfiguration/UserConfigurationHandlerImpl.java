package com.smartverse.churchlitebackend.handlers.userconfiguration;

import com.smartverse.churchlitebackend.services.userconfiguration.UserConfigurationService;
import com.smartverse.churchlitebackend_gen.UserConfigurationDTO;
import com.smartverse.churchlitebackend_gen.UserConfigurationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserConfigurationHandlerImpl extends UserConfigurationHandler  {

    @Autowired
    UserConfigurationService userConfigurationService;

    @Override
    @Transactional
    public UserConfigurationDTO update(UserConfigurationDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        var dto = dtoConverter.toDTO(entity, null);
        entityManager.flush();
        userConfigurationService.updateMaster(entity);
        return dto;
    }
}
