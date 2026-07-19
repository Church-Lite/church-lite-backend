package com.smartverse.churchlitebackend.service.appointments;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.converters.AppointmentsDTOConverter;
import com.smartverse.churchlitebackend_gen.dtos.AppointmentsDTO;
import com.smartverse.churchlitebackend_gen.enums.AppointmentStatus;
import com.smartverse.churchlitebackend_gen.enums.RecurrenceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentsBusinessService {

    private static final int MAX_RECURRENCE_DAYS = 730;

    @PersistenceContext
    private EntityManager entityManager;

    private final AppointmentsDTOConverter dtoConverter;

    public AppointmentsBusinessService(AppointmentsDTOConverter dtoConverter) {
        this.dtoConverter = dtoConverter;
    }

    @Transactional
    public List<AppointmentsDTO> createRecurring(AppointmentsDTO appointment) {
        validateBaseAppointment(appointment);

        appointment.setStatus(AppointmentStatus.SCHEDULED);
        if (appointment.getRecurrenceType() == null) {
            appointment.setRecurrenceType(RecurrenceType.NONE);
        }

        if (appointment.getRecurrenceType() == RecurrenceType.NONE) {
            appointment.setRecurrenceDays(null);
            appointment.setRecurrenceEndDate(null);
            appointment.setRecurrenceGroupId(null);
            return List.of(persist(appointment));
        }

        return createWeeklyOccurrences(appointment);
    }

    private List<AppointmentsDTO> createWeeklyOccurrences(AppointmentsDTO appointment) {
        LocalDate firstDate = appointment.getInitialDate().toLocalDate();
        LocalDate endDate = appointment.getRecurrenceEndDate();

        if (endDate == null || endDate.isBefore(firstDate)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "A data final da recorrência deve ser igual ou posterior à data inicial");
        }
        if (firstDate.plusDays(MAX_RECURRENCE_DAYS).isBefore(endDate)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "A recorrência não pode ultrapassar dois anos");
        }

        EnumSet<DayOfWeek> weekDays = parseWeekDays(appointment.getRecurrenceDays());
        UUID groupId = UUID.randomUUID();
        Duration duration = Duration.between(appointment.getInitialDate(), appointment.getFinalDate());
        List<AppointmentsDTO> created = new ArrayList<>();

        for (LocalDate date = firstDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!weekDays.contains(date.getDayOfWeek())) {
                continue;
            }

            AppointmentsDTO occurrence = copyOf(appointment);
            LocalDateTime start = date.atTime(appointment.getInitialDate().toLocalTime());
            occurrence.setInitialDate(start);
            occurrence.setFinalDate(start.plus(duration));
            occurrence.setRecurrenceGroupId(groupId);
            created.add(persist(occurrence));
        }

        if (created.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Nenhuma ocorrência foi encontrada para os dias selecionados");
        }

        return created;
    }

    private EnumSet<DayOfWeek> parseWeekDays(String recurrenceDays) {
        if (recurrenceDays == null || recurrenceDays.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Selecione pelo menos um dia da semana");
        }

        try {
            return Arrays.stream(recurrenceDays.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "A recorrência possui um dia da semana inválido");
        }
    }

    private void validateBaseAppointment(AppointmentsDTO appointment) {
        if (appointment.getEventsType() == null || appointment.getEventsType().getId() == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "O tipo de evento é obrigatório");
        }
        if (appointment.getUserConfiguration() == null || appointment.getUserConfiguration().getId() == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "O usuário é obrigatório");
        }
        if (appointment.getInitialDate() == null || appointment.getFinalDate() == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "As datas inicial e final são obrigatórias");
        }
        if (!appointment.getFinalDate().isAfter(appointment.getInitialDate())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "A data final deve ser posterior à data inicial");
        }
    }

    private AppointmentsDTO persist(AppointmentsDTO appointment) {
        appointment.setId(null);
        var entity = dtoConverter.toEntity(appointment, null);
        entityManager.persist(entity);
        entityManager.flush();
        return dtoConverter.toDTO(entity, null);
    }

    private AppointmentsDTO copyOf(AppointmentsDTO source) {
        AppointmentsDTO copy = new AppointmentsDTO();
        copy.setEventsType(source.getEventsType());
        copy.setUserConfiguration(source.getUserConfiguration());
        copy.setInitialDate(source.getInitialDate());
        copy.setFinalDate(source.getFinalDate());
        copy.setLocal(source.getLocal());
        copy.setDescription(source.getDescription());
        copy.setStatus(AppointmentStatus.SCHEDULED);
        copy.setRecurrenceType(RecurrenceType.WEEKLY);
        copy.setRecurrenceDays(source.getRecurrenceDays());
        copy.setRecurrenceEndDate(source.getRecurrenceEndDate());
        return copy;
    }
}
