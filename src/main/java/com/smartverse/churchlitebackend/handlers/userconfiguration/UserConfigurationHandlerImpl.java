package com.smartverse.churchlitebackend.handlers.userconfiguration;

import com.smartverse.churchlitebackend_gen.UserConfigurationDTO;
import com.smartverse.churchlitebackend_gen.UserConfigurationHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserConfigurationHandlerImpl extends UserConfigurationHandler  {

    @Override
    public UserConfigurationDTO update(UserConfigurationDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);
        entityManager.merge(entity);
        return dtoConverter.toDTO(entity, null);
    }
}
