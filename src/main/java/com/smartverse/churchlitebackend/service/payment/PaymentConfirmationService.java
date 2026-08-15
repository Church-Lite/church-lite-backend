package com.smartverse.churchlitebackend.service.payment;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.repository.payment.PaymentEventInboxCustomRepository;
import com.smartverse.churchlitebackend.repository.payment.SubscriptionPaymentCustomRepository;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionCache;
import com.smartverse.churchlitebackend_gen.dtos.PaymentConfirmedEventDTO;
import com.smartverse.churchlitebackend_gen.entities.PaymentEventInboxEntity;
import com.smartverse.churchlitebackend_gen.entities.SubscriptionPaymentEntity;
import com.smartverse.churchlitebackend_gen.entities.TenantSubscriptionEntity;
import com.smartverse.churchlitebackend_gen.enums.PaymentStatus;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPlanRepository;
import com.smartverse.churchlitebackend_gen.repositories.TenantSubscriptionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PaymentConfirmationService {
    private static final String ADMIN_TENANT = "admin";
    private static final String SERVICE = "CHURCH_LITE";
    private static final String RECEIVED = "RECEIVED";
    private static final String PROCESSED = "PROCESSED";
    private static final String FAILED = "FAILED";

    private final PaymentEventInboxCustomRepository inboxRepository;
    private final SubscriptionPaymentCustomRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionCache subscriptionCache;
    private final DBMigration dbMigration;
    private final TenantSchemaInterceptor tenantSchemaInterceptor;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public PaymentConfirmationService(
            PaymentEventInboxCustomRepository inboxRepository,
            SubscriptionPaymentCustomRepository paymentRepository,
            SubscriptionPlanRepository planRepository,
            TenantSubscriptionRepository subscriptionRepository,
            SubscriptionCache subscriptionCache,
            DBMigration dbMigration,
            TenantSchemaInterceptor tenantSchemaInterceptor,
            EntityManager entityManager,
            TransactionTemplate transactionTemplate) {
        this.inboxRepository = inboxRepository;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionCache = subscriptionCache;
        this.dbMigration = dbMigration;
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public void receive(PaymentConfirmedEventDTO event) {
        validate(event);
        if (registerEvent(event)) {
            return;
        }

        try {
            String tenant = inAdminTransaction(() -> applyPayment(event));
            subscriptionCache.evict(tenant);
        } catch (RuntimeException exception) {
            markFailed(event.paymentId, exception);
            throw exception;
        }
    }

    @Scheduled(
            fixedDelayString = "${smart-payment.retry-delay-ms:60000}",
            initialDelayString = "${smart-payment.retry-initial-delay-ms:60000}")
    public void retryFailedEvents() {
        var failedEvents = inAdminTransaction(() -> inboxRepository.findAllByStatus(FAILED));
        failedEvents.forEach(stored -> {
            var event = new PaymentConfirmedEventDTO();
            event.paymentId = stored.getPaymentId();
            event.clientId = stored.getClientId();
            event.service = SERVICE;
            event.orderNsu = stored.getOrderNsu();
            event.transactionNsu = stored.getTransactionNsu();
            event.amount = stored.getAmount();
            event.paidAmount = stored.getPaidAmount();
            event.paidAt = stored.getPaidAt();
            try {
                receive(event);
            } catch (RuntimeException ignored) {
                // A causa e o estado FAILED permanecem registrados no inbox.
            }
        });
    }

    private boolean registerEvent(PaymentConfirmedEventDTO event) {
        try {
            return Boolean.TRUE.equals(inAdminTransaction(() -> {
                var existing = inboxRepository.findByPaymentId(event.paymentId);
                if (existing.isPresent()) {
                    return PROCESSED.equals(existing.get().getStatus());
                }
                var sameTransaction = inboxRepository.findByTransactionNsu(event.transactionNsu);
                if (sameTransaction.isPresent()) {
                    throw new ServiceException(
                            HttpStatus.CONFLICT, "payment_transaction_already_received");
                }
                var stored = new PaymentEventInboxEntity();
                stored.setPaymentId(event.paymentId);
                stored.setClientId(event.clientId);
                stored.setOrderNsu(event.orderNsu);
                stored.setTransactionNsu(event.transactionNsu);
                stored.setStatus(RECEIVED);
                stored.setAmount(event.amount);
                stored.setPaidAmount(event.paidAmount);
                stored.setPaidAt(event.paidAt);
                stored.setReceivedAt(LocalDateTime.now());
                inboxRepository.saveAndFlush(stored);
                return false;
            }));
        } catch (DataIntegrityViolationException exception) {
            return Boolean.TRUE.equals(inAdminTransaction(() -> inboxRepository
                    .findByPaymentId(event.paymentId)
                    .map(stored -> PROCESSED.equals(stored.getStatus()))
                    .orElseThrow(() -> new ServiceException(
                            HttpStatus.CONFLICT, "payment_event_conflict"))));
        }
    }

    private String applyPayment(PaymentConfirmedEventDTO event) {
        SubscriptionPaymentEntity payment = paymentRepository.findById(event.clientId)
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.NOT_FOUND, "payment_charge_not_found"));
        validatePaymentMatch(payment, event);
        if (payment.getStatus() == PaymentStatus.PAID) {
            markProcessed(event.paymentId);
            return payment.getTenant();
        }

        String tenant = payment.getTenant();
        String planCode = payment.getPlanCode();
        int months = payment.getMonths();
        switchTenantInsideTransaction(tenant);

        var plan = findActivePlan(planCode);
        var subscription = findOrCreateSubscription(tenant);
        LocalDateTime coverageStart = subscription.getCurrentPeriodEndsAt() != null
                && subscription.getCurrentPeriodEndsAt().isAfter(event.paidAt)
                ? subscription.getCurrentPeriodEndsAt()
                : event.paidAt;
        LocalDateTime coverageEnd = coverageStart.plusMonths(months);
        applySubscription(subscription, plan, event.paidAt, coverageEnd);
        subscriptionRepository.save(subscription);

        switchTenantInsideTransaction(ADMIN_TENANT);
        var administrativePlan = findActivePlan(planCode);
        var administrativeSubscription = findOrCreateSubscription(tenant);
        applySubscription(
                administrativeSubscription,
                administrativePlan,
                event.paidAt,
                coverageEnd);
        subscriptionRepository.save(administrativeSubscription);

        var storedPayment = paymentRepository.findById(event.clientId)
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.NOT_FOUND, "payment_charge_not_found"));
        storedPayment.setStatus(PaymentStatus.PAID);
        storedPayment.setTransactionNsu(event.transactionNsu);
        storedPayment.setPaidAt(event.paidAt);
        storedPayment.setCoverageStartAt(coverageStart);
        storedPayment.setCoverageEndAt(coverageEnd);
        storedPayment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(storedPayment);
        markProcessed(event.paymentId);
        return tenant;
    }

    private void validatePaymentMatch(
            SubscriptionPaymentEntity payment,
            PaymentConfirmedEventDTO event) {
        if (payment.getOrderNsu() == null
                || !payment.getOrderNsu().equals(event.orderNsu)
                || !payment.getAmountCents().equals(event.amount)
                || event.paidAmount < payment.getAmountCents()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "payment_event_does_not_match_charge");
        }
    }

    private com.smartverse.churchlitebackend_gen.entities.SubscriptionPlanEntity findActivePlan(
            String planCode) {
        return planRepository.findAll().stream()
                .filter(item -> planCode.equalsIgnoreCase(item.getCode()) && item.isActive())
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.NOT_FOUND, "subscription_plan_not_found"));
    }

    private TenantSubscriptionEntity findOrCreateSubscription(String tenant) {
        return subscriptionRepository.findAll().stream()
                .filter(item -> tenant.equalsIgnoreCase(item.getTenant()))
                .findFirst()
                .orElseGet(() -> {
                    var created = new TenantSubscriptionEntity();
                    created.setTenant(tenant);
                    return created;
                });
    }

    private void applySubscription(
            TenantSubscriptionEntity subscription,
            com.smartverse.churchlitebackend_gen.entities.SubscriptionPlanEntity plan,
            LocalDateTime paidAt,
            LocalDateTime coverageEnd) {
        subscription.setPlan(plan);
        subscription.setStatus("ACTIVE");
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(paidAt);
        }
        subscription.setCurrentPeriodEndsAt(coverageEnd);
        subscription.setGracePeriodEndsAt(null);
        subscription.setPriceMonthly(plan.getPriceMonthly());
    }

    private void markProcessed(UUID paymentId) {
        var stored = inboxRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.NOT_FOUND, "payment_event_not_found"));
        stored.setStatus(PROCESSED);
        stored.setFailureReason(null);
        stored.setProcessedAt(LocalDateTime.now());
        inboxRepository.save(stored);
    }

    private void markFailed(UUID paymentId, RuntimeException exception) {
        try {
            inAdminTransaction(() -> {
                inboxRepository.findByPaymentId(paymentId).ifPresent(stored -> {
                    stored.setStatus(FAILED);
                    stored.setFailureReason(safeMessage(exception));
                    inboxRepository.save(stored);
                });
                return null;
            });
        } catch (RuntimeException ignored) {
            // Preserva a excecao original; o evento continuara visivel nos logs do listener.
        }
    }

    private void validate(PaymentConfirmedEventDTO event) {
        if (event == null || event.paymentId == null || event.clientId == null
                || event.orderNsu == null || event.orderNsu.isBlank()
                || event.transactionNsu == null || event.transactionNsu.isBlank()
                || event.amount == null || event.amount <= 0
                || event.paidAmount == null || event.paidAmount <= 0
                || event.paidAt == null || !SERVICE.equals(event.service)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "payment_confirmed_event_invalid");
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }

    private <T> T inAdminTransaction(Supplier<T> operation) {
        String previousTenant = TenantContext.getCurrentTenant();
        try {
            selectTenant(ADMIN_TENANT);
            return transactionTemplate.execute(status -> operation.get());
        } finally {
            entityManager.clear();
            TenantContext.setCurrentTenant(previousTenant);
        }
    }

    private void selectTenant(String tenant) {
        dbMigration.loadMigrateTenants(tenant);
        TenantContext.setCurrentTenant(tenant);
        tenantSchemaInterceptor.switchSchema();
    }

    private void switchTenantInsideTransaction(String tenant) {
        entityManager.flush();
        entityManager.clear();
        selectTenant(tenant);
    }
}
