package com.smartverse.churchlitebackend.service.financial;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.RecurringFinancialDTO;
import com.smartverse.churchlitebackend_gen.entities.FinancialEntity;
import com.smartverse.churchlitebackend_gen.entities.RecurringFinancialEntity;
import com.smartverse.churchlitebackend_gen.enums.FinancialRecurrenceFrequency;
import com.smartverse.churchlitebackend_gen.enums.FinancialRecurrenceMode;
import com.smartverse.churchlitebackend_gen.enums.FinancialRecurrenceStatus;
import com.smartverse.churchlitebackend_gen.services.RecurringFinancialService;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecurringFinancialBusinessService extends RecurringFinancialService {

    private static final int MAX_OCCURRENCES = 60;
    private static final int CONTINUOUS_WINDOW = 12;

    private final EntityManager entityManager;

    public RecurringFinancialBusinessService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public RecurringFinancialDTO save(RecurringFinancialDTO dto) {
        validate(dto);

        var recurrence = dtoConverter.toEntity(dto, null);
        recurrence.setId(null);
        recurrence.setStatus(FinancialRecurrenceStatus.ACTIVE);
        recurrence.setGeneratedOccurrences(0);
        entityManager.persist(recurrence);

        var dueDates = calculateDueDates(dto);
        for (int index = 0; index < dueDates.size(); index++) {
            entityManager.persist(toFinancial(recurrence, dueDates.get(index), index + 1));
        }
        recurrence.setGeneratedOccurrences(dueDates.size());
        return dtoConverter.toDTO(recurrence, null);
    }

    @Override
    @Transactional
    public RecurringFinancialDTO update(RecurringFinancialDTO dto, UUID id) {
        var current = repository.findById(id)
                .orElseThrow(() -> error("financial_recurrence_not_found"));

        if (dto.getStatus() == null) throw error("financial_recurrence_status_required");
        current.setStatus(dto.getStatus());

        // A programação é imutável depois de gerada. Dados financeiros podem ser
        // corrigidos e são aplicados somente às ocorrências futuras ainda abertas.
        current.setDescription(required(dto.getDescription(), "financial_recurrence_description_required"));
        current.setValue(positive(dto.getValue()));
        current.setValueType(dto.getValueType());
        current.setCash(dtoConverter.toEntity(dto, null).getCash());
        current.setPerson(dtoConverter.toEntity(dto, null).getPerson());
        current.setPlanAccount(dtoConverter.toEntity(dto, null).getPlanAccount());
        current.setCostCenter(dtoConverter.toEntity(dto, null).getCostCenter());

        entityManager.createQuery("""
                        update FinancialEntity f
                           set f.description = :description,
                               f.value = :value,
                               f.cash = :cash,
                               f.person = :person,
                               f.planAccount = :planAccount,
                               f.costCenter = :costCenter
                         where f.recurringFinancial.id = :id
                           and f.paymentReceiptDate is null
                           and f.dueDate >= :today
                        """)
                .setParameter("description", current.getDescription())
                .setParameter("value", current.getValue())
                .setParameter("cash", current.getCash())
                .setParameter("person", current.getPerson())
                .setParameter("planAccount", current.getPlanAccount())
                .setParameter("costCenter", current.getCostCenter())
                .setParameter("id", id)
                .setParameter("today", LocalDate.now())
                .executeUpdate();

        return dtoConverter.toDTO(current, null);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var paid = entityManager.createQuery("""
                        select count(f) from FinancialEntity f
                         where f.recurringFinancial.id = :id
                           and f.paymentReceiptDate is not null
                        """, Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (paid > 0) throw error("financial_recurrence_has_settled_entries");

        entityManager.createQuery("delete from FinancialEntity f where f.recurringFinancial.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        repository.deleteById(id);
    }

    private void validate(RecurringFinancialDTO dto) {
        required(dto.getDescription(), "financial_recurrence_description_required");
        positive(dto.getValue());
        if (dto.getTypeFinancial() == null || dto.getRecurrenceMode() == null || dto.getFrequency() == null
                || dto.getValueType() == null || dto.getFirstDueDate() == null || dto.getCash() == null
                || dto.getPlanAccount() == null || dto.getCostCenter() == null) {
            throw error("financial_recurrence_required_fields");
        }
        var planAccount = entityManager.find(com.smartverse.churchlitebackend_gen.entities.PlanAccountEntity.class, dto.getPlanAccount().getId());
        if (planAccount == null || !planAccount.getFinancialNature().name().equals(dto.getTypeFinancial().name())) {
            throw error("financial_plan_account_nature_mismatch");
        }
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getFirstDueDate())) {
            throw error("financial_recurrence_invalid_end_date");
        }
        if (dto.getRecurrenceMode() == FinancialRecurrenceMode.INSTALLMENT
                && (dto.getOccurrenceCount() == null || dto.getOccurrenceCount() < 2 || dto.getOccurrenceCount() > MAX_OCCURRENCES)) {
            throw error("financial_recurrence_installments_between_2_and_60");
        }
    }

    private List<LocalDate> calculateDueDates(RecurringFinancialDTO dto) {
        int limit = dto.getRecurrenceMode() == FinancialRecurrenceMode.INSTALLMENT
                ? dto.getOccurrenceCount()
                : dto.getEndDate() == null ? CONTINUOUS_WINDOW : MAX_OCCURRENCES;
        var dates = new ArrayList<LocalDate>();
        var date = dto.getFirstDueDate();
        while (dates.size() < limit && dates.size() < MAX_OCCURRENCES
                && (dto.getEndDate() == null || !date.isAfter(dto.getEndDate()))) {
            dates.add(date);
            date = next(date, dto.getFrequency());
        }
        if (dates.isEmpty()) throw error("financial_recurrence_without_occurrences");
        return dates;
    }

    private LocalDate next(LocalDate date, FinancialRecurrenceFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case BIMONTHLY -> date.plusMonths(2);
            case QUARTERLY -> date.plusMonths(3);
            case SEMIANNUAL -> date.plusMonths(6);
            case ANNUAL -> date.plusYears(1);
        };
    }

    private FinancialEntity toFinancial(RecurringFinancialEntity recurrence, LocalDate dueDate, int number) {
        var financial = new FinancialEntity();
        financial.setDescription(recurrence.getDescription());
        financial.setTypeFinancial(recurrence.getTypeFinancial());
        financial.setCash(recurrence.getCash());
        financial.setValue(recurrence.getValue());
        financial.setPerson(recurrence.getPerson());
        financial.setPlanAccount(recurrence.getPlanAccount());
        financial.setCostCenter(recurrence.getCostCenter());
        financial.setIssueDate(LocalDate.now());
        financial.setDueDate(dueDate);
        financial.setRecurringFinancial(recurrence);
        financial.setRecurrenceNumber(number);
        return financial;
    }

    private String required(String value, String key) {
        if (value == null || value.isBlank()) throw error(key);
        return value.trim();
    }

    private Double positive(Double value) {
        if (value == null || value <= 0) throw error("financial_recurrence_positive_value");
        return value;
    }

    private ServiceException error(String key) {
        return new ServiceException(HttpStatus.UNPROCESSABLE_ENTITY, key);
    }
}
