package com.smartverse.churchlitebackend.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CellVisitorHandlerImpl extends CellVisitorHandler {
    @Override @Transactional
    public CellVisitorDTO save(CellVisitorDTO dto) { validate(dto); return super.save(dto); }

    @Override @Transactional
    public CellVisitorDTO update(CellVisitorDTO dto, UUID id) { validate(dto); return super.update(dto, id); }

    private void validate(CellVisitorDTO dto) {
        if (dto.getCell() == null || dto.getCell().getId() == null || dto.getName() == null || dto.getName().isBlank())
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Célula e nome do visitante são obrigatórios");
        if (dto.getFirstVisitDate() == null || dto.getLastVisitDate() == null || dto.getLastVisitDate().isBefore(dto.getFirstVisitDate()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "As datas de visita são inválidas");
        if (dto.getVisitCount() == null || dto.getVisitCount() < 1)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "A quantidade de visitas deve ser maior que zero");
        if (dto.getStatus() == null) dto.setStatus(CellVisitorStatus.NEW);
        dto.setName(dto.getName().trim());
    }
}
