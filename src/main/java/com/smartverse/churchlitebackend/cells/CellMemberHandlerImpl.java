package com.smartverse.churchlitebackend.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
public class CellMemberHandlerImpl extends CellMemberHandler {
    @Override @Transactional public CellMemberDTO save(CellMemberDTO dto) { validate(dto, null); return super.save(dto); }
    @Override @Transactional public CellMemberDTO update(CellMemberDTO dto, UUID id) { validate(dto, id); return super.update(dto, id); }
    @Override @Transactional public void delete(UUID id) { throw new ServiceException(HttpStatus.BAD_REQUEST, "Vínculos devem ser encerrados, não excluídos"); }

    private void validate(CellMemberDTO dto, UUID id) {
        if (dto.getCell() == null || dto.getPerson() == null || dto.getEntryDate() == null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Célula, pessoa e data de entrada são obrigatórios");
        if (dto.getExitDate() != null && dto.getExitDate().isBefore(dto.getEntryDate()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Data de saída anterior à entrada");
        if (dto.getStatus() != CellMemberStatus.ACTIVE || dto.getExitDate() != null) return;
        boolean duplicate = repository.findAll().stream().anyMatch(item -> !item.getId().equals(id) && item.getCell().getId().equals(dto.getCell().getId()) && item.getPerson().getId().equals(dto.getPerson().getId()) && item.getStatus() == CellMemberStatus.ACTIVE && item.getExitDate() == null);
        if (duplicate) throw new ServiceException(HttpStatus.CONFLICT, "A pessoa já possui vínculo ativo nesta célula");
        if (dto.isPrimaryCell()) {
            boolean primary = repository.findAll().stream().anyMatch(item -> !item.getId().equals(id) && item.getPerson().getId().equals(dto.getPerson().getId()) && item.isPrimaryCell() && item.getStatus() == CellMemberStatus.ACTIVE && item.getExitDate() == null);
            if (primary) throw new ServiceException(HttpStatus.CONFLICT, "A pessoa já possui outra célula principal ativa");
        }
    }
}
