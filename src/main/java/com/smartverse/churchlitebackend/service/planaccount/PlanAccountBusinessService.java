package com.smartverse.churchlitebackend.service.planaccount;

import com.smartverse.churchlitebackend_gen.dtos.PlanAccountDTO;
import com.smartverse.churchlitebackend_gen.services.PlanAccountService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlanAccountBusinessService extends PlanAccountService {

    private final EntityManager entityManager;

    public PlanAccountBusinessService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public PlanAccountDTO update(PlanAccountDTO obj, UUID id) {
        var entity = dtoConverter.toEntity(obj, null);
        entity.setId(id);

        var hasChildren = repository.findById(id).orElse(null);
        if (hasChildren != null) {
            entity.setChildren(hasChildren.getChildren());
        }

        entityManager.merge(entity);
        return dtoConverter.toDTO(entity, null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var entity = repository.findById(id).orElse(null);
        if (entity != null) {
            if (entity.getParentCode() != null) {
                var parent = repository.findById(entity.getParentCode().getId()).orElse(null);
                parent.getChildren().remove(entity);
                entityManager.merge(parent);
            } else {
                entityManager.remove(entity);
            }
        }
    }
}
