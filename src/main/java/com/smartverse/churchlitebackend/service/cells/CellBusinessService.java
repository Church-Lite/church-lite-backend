package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.dtos.CellDTO;
import com.smartverse.churchlitebackend_gen.enums.CellStatus;
import com.smartverse.churchlitebackend_gen.services.CellService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CellBusinessService extends CellService {
    private final SubscriptionService subscriptionService;

    public CellBusinessService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public CellDTO save(CellDTO dto) {
        if (dto.getStatus() == CellStatus.ACTIVE) {
            subscriptionService.requireAvailable(SubscriptionResource.ACTIVE_CELL);
        }
        validate(dto, null);
        return super.save(dto);
    }

    @Override
    @Transactional
    public CellDTO update(CellDTO dto, UUID id) {
        boolean activating = dto.getStatus() == CellStatus.ACTIVE
                && repository.findById(id)
                .map(item -> item.getStatus() != CellStatus.ACTIVE)
                .orElse(true);
        if (activating) {
            subscriptionService.requireAvailable(SubscriptionResource.ACTIVE_CELL);
        }
        validate(dto, id);
        return super.update(dto, id);
    }

    private void validate(CellDTO dto, UUID id) {
        if (dto.getCode() == null || dto.getCode().isBlank() || dto.getName() == null || dto.getName().isBlank())
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Código e nome da célula são obrigatórios");
        boolean duplicate = repository.findAll().stream().anyMatch(item -> item.getCode().equalsIgnoreCase(dto.getCode().trim()) && !item.getId().equals(id));
        if (duplicate) throw new ServiceException(HttpStatus.CONFLICT, "Já existe uma célula com este código");
        if (dto.getStatus() == CellStatus.ACTIVE && (dto.getMeetingDay() == null || dto.getMeetingTime() == null || dto.getMeetingTime().isBlank()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Uma célula ativa deve possuir dia e horário de reunião");
        if (dto.getMeetingDay() != null && (dto.getMeetingDay() < 0 || dto.getMeetingDay() > 6))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Dia da semana inválido");
        dto.setCode(dto.getCode().trim());
        dto.setName(dto.getName().trim());
    }
}
