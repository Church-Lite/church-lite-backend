package com.smartverse.churchlitebackend.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class CellMeetingWorkflowHandlerImpl implements SubmitCellMeeting, ReviewCellMeeting {
    private final CellMeetingRepository repository;
    private final CellMeetingDTOConverter converter;

    public CellMeetingWorkflowHandlerImpl(CellMeetingRepository repository, CellMeetingDTOConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    @Override @Transactional
    public ResponseEntity<SubmitCellMeetingOutput> submitCellMeeting(SubmitCellMeetingInput input) {
        if (input == null || input.meetingId == null) throw new ServiceException(HttpStatus.BAD_REQUEST, "A reunião é obrigatória");
        var meeting = repository.findById(input.meetingId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Reunião não encontrada"));
        if (meeting.getStatus() != CellMeetingStatus.DRAFT && meeting.getStatus() != CellMeetingStatus.REJECTED)
            throw new ServiceException(HttpStatus.CONFLICT, "A reunião não está disponível para submissão");
        meeting.setStatus(CellMeetingStatus.SUBMITTED);
        meeting.setSubmittedAt(LocalDateTime.now());
        meeting.setReviewedAt(null);
        meeting.setReviewReason(null);
        var output = new SubmitCellMeetingOutput();
        output.meeting = converter.toDTO(meeting, null);
        return ResponseEntity.ok(output);
    }

    @Override @Transactional
    public ResponseEntity<ReviewCellMeetingOutput> reviewCellMeeting(ReviewCellMeetingInput input) {
        if (input == null || input.meetingId == null) throw new ServiceException(HttpStatus.BAD_REQUEST, "A reunião é obrigatória");
        var meeting = repository.findById(input.meetingId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Reunião não encontrada"));
        if (meeting.getStatus() != CellMeetingStatus.SUBMITTED)
            throw new ServiceException(HttpStatus.CONFLICT, "Somente reuniões submetidas podem ser revisadas");
        if (!input.approved && (input.reason == null || input.reason.isBlank()))
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Informe o motivo da rejeição");
        meeting.setStatus(input.approved ? CellMeetingStatus.APPROVED : CellMeetingStatus.REJECTED);
        meeting.setReviewedAt(LocalDateTime.now());
        meeting.setReviewReason(input.reason == null ? null : input.reason.trim());
        var output = new ReviewCellMeetingOutput();
        output.meeting = converter.toDTO(meeting, null);
        return ResponseEntity.ok(output);
    }
}
