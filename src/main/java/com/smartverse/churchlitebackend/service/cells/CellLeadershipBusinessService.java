package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.CellLeadershipDTO;
import com.smartverse.churchlitebackend_gen.enums.Status;
import com.smartverse.churchlitebackend_gen.services.CellLeadershipService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CellLeadershipBusinessService extends CellLeadershipService {
    @Override
    @Transactional
    public CellLeadershipDTO save(CellLeadershipDTO dto) {
        validate(dto, null);
        return super.save(dto);
    }

    @Override
    @Transactional
    public CellLeadershipDTO update(CellLeadershipDTO dto, UUID id) {
        validate(dto, id);
        return super.update(dto, id);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        throw new ServiceException(HttpStatus.BAD_REQUEST, "Lideranças devem ser encerradas, não excluídas");
    }

    private void validate(CellLeadershipDTO dto, UUID id) {
        if (dto.getCell() == null || dto.getPerson() == null || dto.getRole() == null || dto.getStartDate() == null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Célula, pessoa, papel e data de início são obrigatórios");
        if (dto.getPerson().getStatus() != Status.ACTIVE)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "O líder deve ser uma pessoa ativa");
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Data final anterior à inicial");
        if (!dto.isActive() || dto.getEndDate() != null) return;
        boolean duplicate = repository.findAll().stream().anyMatch(item -> !item.getId().equals(id) && item.getCell().getId().equals(dto.getCell().getId()) && item.getPerson().getId().equals(dto.getPerson().getId()) && item.getRole() == dto.getRole() && item.isActive() && item.getEndDate() == null);
        if (duplicate) throw new ServiceException(HttpStatus.CONFLICT, "Esta liderança ativa já está cadastrada");
    }
}
