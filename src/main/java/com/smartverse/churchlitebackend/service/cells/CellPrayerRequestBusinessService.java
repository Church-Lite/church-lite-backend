package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.CellPrayerRequestDTO;
import com.smartverse.churchlitebackend_gen.enums.CellPrayerRequestStatus;
import com.smartverse.churchlitebackend_gen.services.CellPrayerRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CellPrayerRequestBusinessService extends CellPrayerRequestService {
    @Override
    @Transactional
    public CellPrayerRequestDTO save(CellPrayerRequestDTO dto) {
        validate(dto);
        return super.save(dto);
    }

    @Override
    @Transactional
    public CellPrayerRequestDTO update(CellPrayerRequestDTO dto, UUID id) {
        validate(dto);
        return super.update(dto, id);
    }

    private void validate(CellPrayerRequestDTO dto) {
        if (dto.getMeeting() == null || dto.getMeeting().getId() == null || dto.getDescription() == null || dto.getDescription().isBlank())
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Reunião e descrição do pedido são obrigatórios");
        if (dto.getPerson() != null && dto.getPerson().getId() != null && dto.getVisitor() != null && dto.getVisitor().getId() != null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "O pedido deve pertencer a uma pessoa ou visitante, não aos dois");
        if (dto.getStatus() == null) dto.setStatus(CellPrayerRequestStatus.OPEN);
        dto.setDescription(dto.getDescription().trim());
    }
}
