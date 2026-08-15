package com.smartverse.churchlitebackend.repository.report;

import com.smartverse.churchlitebackend_gen.entities.IntegrationConfigurationEntity;
import com.smartverse.churchlitebackend_gen.enums.IntegrationService;
import com.smartverse.churchlitebackend_gen.repositories.IntegrationConfigurationRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface IntegrationConfigurationCustomRepository extends IntegrationConfigurationRepository {
    Optional<IntegrationConfigurationEntity> findByService(IntegrationService service);
}
