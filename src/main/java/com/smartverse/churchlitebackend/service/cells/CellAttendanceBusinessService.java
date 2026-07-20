package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.CellAttendanceDTO;
import com.smartverse.churchlitebackend_gen.services.CellAttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CellAttendanceBusinessService extends CellAttendanceService {
    @Override
    @Transactional
    public CellAttendanceDTO save(CellAttendanceDTO dto) {
        validate(dto, null);
        return super.save(dto);
    }

    @Override
    @Transactional
    public CellAttendanceDTO update(CellAttendanceDTO dto, UUID id) {
        validate(dto, id);
        return super.update(dto, id);
    }

    private void validate(CellAttendanceDTO dto, UUID id) {
        if (dto.getMeeting() == null || dto.getMeeting().getId() == null || dto.getType() == null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Reunião e tipo de presença são obrigatórios");
        boolean hasPerson = dto.getPerson() != null && dto.getPerson().getId() != null;
        boolean hasVisitor = dto.getVisitor() != null && dto.getVisitor().getId() != null;
        if (hasPerson == hasVisitor)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Informe uma pessoa ou um visitante para a presença");
        boolean duplicate = repository.findAll().stream().anyMatch(item -> !item.getId().equals(id)
                && item.getMeeting().getId().equals(dto.getMeeting().getId())
                && ((hasPerson && item.getPerson() != null && item.getPerson().getId().equals(dto.getPerson().getId()))
                || (hasVisitor && item.getVisitor() != null && item.getVisitor().getId().equals(dto.getVisitor().getId()))));
        if (duplicate) throw new ServiceException(HttpStatus.CONFLICT, "Esta presença já foi registrada na reunião");
    }
}
