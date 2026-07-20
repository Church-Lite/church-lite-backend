package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.CellMeetingDTO;
import com.smartverse.churchlitebackend_gen.enums.CellMeetingStatus;
import com.smartverse.churchlitebackend_gen.services.CellMeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CellMeetingBusinessService extends CellMeetingService {
    @Override
    @Transactional
    public CellMeetingDTO save(CellMeetingDTO dto) {
        validate(dto, false);
        dto.setStatus(CellMeetingStatus.DRAFT);
        clearWorkflow(dto);
        return super.save(dto);
    }

    @Override
    @Transactional
    public CellMeetingDTO update(CellMeetingDTO dto, UUID id) {
        var current = repository.findById(id).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Reunião não encontrada"));
        if (current.getStatus() == CellMeetingStatus.SUBMITTED || current.getStatus() == CellMeetingStatus.APPROVED || current.getStatus() == CellMeetingStatus.CANCELLED)
            throw new ServiceException(HttpStatus.CONFLICT, "Somente reuniões em rascunho ou rejeitadas podem ser alteradas");
        validate(dto, true);
        dto.setStatus(CellMeetingStatus.DRAFT);
        clearWorkflow(dto);
        return super.update(dto, id);
    }

    private void validate(CellMeetingDTO dto, boolean update) {
        if (dto.getCell() == null || dto.getCell().getId() == null || dto.getResponsibleLeader() == null || dto.getResponsibleLeader().getId() == null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Célula e líder responsável são obrigatórios");
        if (dto.getStartAt() == null || dto.getEndAt() == null || dto.getEndAt().isBefore(dto.getStartAt()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "O período da reunião é inválido");
        if (negative(dto.getAdultCount()) || negative(dto.getChildrenCount()) || negative(dto.getMemberCount()) || negative(dto.getVisitorCount()) || negative(dto.getDecisions()) || negative(dto.getReconciliations()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "As quantidades da reunião não podem ser negativas");
        if (dto.getOffering() != null && dto.getOffering() < 0)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "A oferta não pode ser negativa");
        dto.setAdultCount(defaultZero(dto.getAdultCount()));
        dto.setChildrenCount(defaultZero(dto.getChildrenCount()));
        dto.setMemberCount(defaultZero(dto.getMemberCount()));
        dto.setVisitorCount(defaultZero(dto.getVisitorCount()));
        dto.setDecisions(defaultZero(dto.getDecisions()));
        dto.setReconciliations(defaultZero(dto.getReconciliations()));
    }

    private boolean negative(Integer value) {
        return value != null && value < 0;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void clearWorkflow(CellMeetingDTO dto) {
        dto.setSubmittedAt(null);
        dto.setSubmittedBy(null);
        dto.setReviewedAt(null);
        dto.setReviewReason(null);
        dto.setCancellationReason(null);
    }
}
