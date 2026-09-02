package com.smartverse.churchlitebackend.service.transactions;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.repository.cash.CashCustomRepository;
import com.smartverse.churchlitebackend.repository.cashtransactions.CashTransactionsCustomRepository;
import com.smartverse.churchlitebackend.repository.transactions.TransactionsCustomRepository;
import com.smartverse.churchlitebackend_gen.entities.CashEntity;
import com.smartverse.churchlitebackend_gen.entities.CashTransactionsEntity;
import com.smartverse.churchlitebackend_gen.entities.TransactionsEntity;
import com.smartverse.churchlitebackend_gen.enums.TransactionOperation;
import com.smartverse.churchlitebackend_gen.enums.TypeCash;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BalanceTransferService {
    private final CashCustomRepository cashRepository;
    private final CashTransactionsCustomRepository sessionRepository;
    private final TransactionsCustomRepository transactionRepository;

    public BalanceTransferService(CashCustomRepository cashRepository,
                                  CashTransactionsCustomRepository sessionRepository,
                                  TransactionsCustomRepository transactionRepository) {
        this.cashRepository = cashRepository;
        this.sessionRepository = sessionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Map<String, Object> create(UUID sourceId, UUID destinationId, Double value,
                                      LocalDate date, String description, String observation) {
        validateRequest(sourceId, destinationId, value, date, description);
        var cashes = lockCashes(sourceId, destinationId);
        var source = cashes.get(sourceId);
        var destination = cashes.get(destinationId);
        var sessions = lockRequiredSessions(source, destination, date);

        if (balance(source, sessions.get(sourceId)) + 0.000001d < value) {
            throw badRequest("Saldo insuficiente na conta de origem");
        }

        UUID transferId = UUID.randomUUID();
        var output = movement(transferId, source, sessions.get(sourceId), value, date, description,
                observation, TransactionOperation.TRANSFER_OUT);
        var input = movement(transferId, destination, sessions.get(destinationId), value, date, description,
                observation, TransactionOperation.TRANSFER_IN);
        transactionRepository.saveAll(List.of(output, input));
        return transferMap(output, input, true);
    }

    @Transactional
    public void reverse(UUID transferId) {
        if (transferId == null) throw badRequest("Transferência não informada");
        var pair = transactionRepository.findByTransferIdOrderByTransactionOperation(transferId);
        if (pair.size() != 2) throw new ServiceException(HttpStatus.NOT_FOUND, "Transferência não encontrada");
        var out = pair.stream().filter(this::isOutput).findFirst().orElseThrow(() -> badRequest("Transferência inconsistente"));
        var in = pair.stream().filter(this::isInput).findFirst().orElseThrow(() -> badRequest("Transferência inconsistente"));
        var cashes = lockCashes(out.getCash().getId(), in.getCash().getId());
        var sessions = lockRequiredSessions(cashes.get(out.getCash().getId()), cashes.get(in.getCash().getId()), null);
        verifySameSession(out, sessions.get(out.getCash().getId()));
        verifySameSession(in, sessions.get(in.getCash().getId()));
        if (balance(cashes.get(in.getCash().getId()), sessions.get(in.getCash().getId())) + 0.000001d < in.getValue()) {
            throw badRequest("O destino não possui saldo suficiente para estornar a transferência");
        }
        transactionRepository.deleteAll(pair);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accounts() {
        return cashRepository.findAll().stream().map(cash -> accountMap(cash, currentSession(cash))).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history() {
        return transactionRepository.findByTransferIdIsNotNullOrderByDateTransactionDescTransferIdDesc().stream()
                .collect(Collectors.groupingBy(TransactionsEntity::getTransferId, LinkedHashMap::new, Collectors.toList()))
                .values().stream().filter(pair -> pair.size() == 2).map(pair -> {
                    var out = pair.stream().filter(this::isOutput).findFirst().orElseThrow();
                    var in = pair.stream().filter(this::isInput).findFirst().orElseThrow();
                    return transferMap(out, in, reversible(out, in));
                }).toList();
    }

    private void validateRequest(UUID sourceId, UUID destinationId, Double value, LocalDate date, String description) {
        if (sourceId == null || destinationId == null) throw badRequest("Origem e destino são obrigatórios");
        if (sourceId.equals(destinationId)) throw badRequest("Origem e destino devem ser diferentes");
        if (value == null || value <= 0) throw badRequest("O valor deve ser maior que zero");
        if (date == null || date.isAfter(LocalDate.now())) throw badRequest("A data não pode ser futura");
        if (description == null || description.isBlank()) throw badRequest("A descrição é obrigatória");
    }

    private Map<UUID, CashEntity> lockCashes(UUID first, UUID second) {
        var result = cashRepository.findAllForUpdate(List.of(first, second));
        if (result.size() != 2) throw new ServiceException(HttpStatus.NOT_FOUND, "Conta de origem ou destino não encontrada");
        return result.stream().collect(Collectors.toMap(CashEntity::getId, Function.identity()));
    }

    private Map<UUID, CashTransactionsEntity> lockRequiredSessions(CashEntity first, CashEntity second, LocalDate date) {
        Map<UUID, CashTransactionsEntity> result = new HashMap<>();
        for (var cash : List.of(first, second)) {
            if (cash.getTypeCash() != TypeCash.CASH) continue;
            var session = sessionRepository.findOpenForUpdate(cash.getId())
                    .orElseThrow(() -> badRequest("O caixa " + cash.getDescription() + " precisa estar aberto"));
            if (date != null && date.isBefore(session.getStartDate())) {
                throw badRequest("A data não pode ser anterior à abertura do caixa " + cash.getDescription());
            }
            result.put(cash.getId(), session);
        }
        return result;
    }

    private CashTransactionsEntity currentSession(CashEntity cash) {
        return cash.getTypeCash() == TypeCash.CASH
                ? sessionRepository.findTopByCashOrderByStartDateDesc(cash).orElse(null) : null;
    }

    private double balance(CashEntity cash, CashTransactionsEntity session) {
        if (cash.getTypeCash() == TypeCash.CASH && session == null) return 0d;
        if (cash.getTypeCash() == TypeCash.CASH && session.getEndDate() != null && session.getFinalBalance() != null) {
            return session.getFinalBalance();
        }
        double base = cash.getTypeCash() == TypeCash.CASH && session.getInitialBalance() != null ? session.getInitialBalance() : 0d;
        UUID sessionId = session == null ? null : session.getId();
        return base + transactionRepository.findByCashId(cash.getId()).stream()
                .filter(item -> cash.getTypeCash() == TypeCash.BANK || Objects.equals(item.getCashTransaction(), sessionId))
                .mapToDouble(this::signedValue).sum();
    }

    private double signedValue(TransactionsEntity item) {
        return switch (item.getTransactionOperation()) {
            case REVENUE, TRANSFER_IN -> item.getValue();
            case EXPENSE, TRANSFER_OUT -> -item.getValue();
            default -> 0d;
        };
    }

    private TransactionsEntity movement(UUID transferId, CashEntity cash, CashTransactionsEntity session,
                                        Double value, LocalDate date, String description, String observation,
                                        TransactionOperation operation) {
        var entity = new TransactionsEntity();
        entity.setTransferId(transferId);
        entity.setCash(cash);
        entity.setCashTransaction(session == null ? null : session.getId());
        entity.setValue(value);
        entity.setDateTransaction(date);
        entity.setDescription(description.trim());
        entity.setObservation(observation == null || observation.isBlank() ? null : observation.trim());
        entity.setTransactionOperation(operation);
        return entity;
    }

    private Map<String, Object> accountMap(CashEntity cash, CashTransactionsEntity session) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", cash.getId());
        item.put("descricao", cash.getDescription());
        item.put("tipo", cash.getTypeCash().name());
        item.put("banco", cash.getBank() == null ? null : cash.getBank().getName());
        item.put("conta", cash.getNumberAccount());
        item.put("aberto", cash.getTypeCash() == TypeCash.BANK || session != null && session.getEndDate() == null);
        item.put("saldo", balance(cash, session));
        return item;
    }

    private Map<String, Object> transferMap(TransactionsEntity out, TransactionsEntity in, boolean reversible) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("transferId", out.getTransferId());
        item.put("dateTransaction", out.getDateTransaction());
        item.put("description", out.getDescription());
        item.put("observation", out.getObservation());
        item.put("value", out.getValue());
        item.put("source", Map.of("id", out.getCash().getId(), "description", out.getCash().getDescription(), "typeCash", out.getCash().getTypeCash().name()));
        item.put("destination", Map.of("id", in.getCash().getId(), "description", in.getCash().getDescription(), "typeCash", in.getCash().getTypeCash().name()));
        item.put("reversible", reversible);
        return item;
    }

    private boolean reversible(TransactionsEntity out, TransactionsEntity in) {
        return sameCurrentSession(out) && sameCurrentSession(in);
    }

    private boolean sameCurrentSession(TransactionsEntity movement) {
        if (movement.getCash().getTypeCash() == TypeCash.BANK) return true;
        return sessionRepository.findTopByCashOrderByStartDateDesc(movement.getCash())
                .filter(session -> session.getEndDate() == null && Objects.equals(session.getId(), movement.getCashTransaction())).isPresent();
    }

    private void verifySameSession(TransactionsEntity movement, CashTransactionsEntity session) {
        if (movement.getCash().getTypeCash() == TypeCash.CASH &&
                (session == null || !Objects.equals(session.getId(), movement.getCashTransaction()))) {
            throw badRequest("A transferência não pode ser estornada após o fechamento do caixa original");
        }
    }

    private boolean isOutput(TransactionsEntity item) { return item.getTransactionOperation() == TransactionOperation.TRANSFER_OUT; }
    private boolean isInput(TransactionsEntity item) { return item.getTransactionOperation() == TransactionOperation.TRANSFER_IN; }
    private ServiceException badRequest(String message) { return new ServiceException(HttpStatus.BAD_REQUEST, message); }
}
