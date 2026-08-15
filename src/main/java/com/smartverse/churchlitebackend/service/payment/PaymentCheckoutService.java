package com.smartverse.churchlitebackend.service.payment;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.client.payment.SmartPaymentClient;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.repository.payment.BillingDiscountCustomRepository;
import com.smartverse.churchlitebackend.repository.payment.SubscriptionPaymentCustomRepository;
import com.smartverse.churchlitebackend_gen.dtos.PaymentHistoryItemDTO;
import com.smartverse.churchlitebackend_gen.endpoints.CreatePaymentLinkOutput;
import com.smartverse.churchlitebackend_gen.endpoints.GetPaymentHistoryOutput;
import com.smartverse.churchlitebackend_gen.entities.SubscriptionPaymentEntity;
import com.smartverse.churchlitebackend_gen.enums.BillingCycle;
import com.smartverse.churchlitebackend_gen.enums.PaymentStatus;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPlanRepository;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
public class PaymentCheckoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCheckoutService.class);
    private static final String ADMIN_TENANT = "admin";
    private static final String SERVICE = "CHURCH_LITE";

    private final SmartPaymentClient smartPaymentClient;
    private final BillingDiscountCustomRepository discountRepository;
    private final SubscriptionPaymentCustomRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final DBMigration dbMigration;
    private final TenantSchemaInterceptor tenantSchemaInterceptor;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public PaymentCheckoutService(
            SmartPaymentClient smartPaymentClient,
            BillingDiscountCustomRepository discountRepository,
            SubscriptionPaymentCustomRepository paymentRepository,
            SubscriptionPlanRepository planRepository,
            DBMigration dbMigration,
            TenantSchemaInterceptor tenantSchemaInterceptor,
            EntityManager entityManager,
            TransactionTemplate transactionTemplate) {
        this.smartPaymentClient = smartPaymentClient;
        this.discountRepository = discountRepository;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.dbMigration = dbMigration;
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public CreatePaymentLinkOutput createLink(
            String planCode,
            BillingCycle billingCycle,
            String authorization) {
        String tenant = requireTenant();
        if (authorization == null || authorization.isBlank()) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "payment_authorization_required");
        }

        SubscriptionPaymentEntity payment = inAdminTransaction(
                () -> preparePayment(tenant, planCode, billingCycle));
        SmartPaymentClient.PaymentLinkResponse provider;
        var request = new SmartPaymentClient.PaymentLinkRequest(
                SERVICE,
                payment.getAmountCents(),
                payment.getId());
        LOGGER.info(
                "Smart Payment request: {{\"service\":\"{}\",\"value\":{},\"client_id\":\"{}\"}}",
                request.service(),
                request.value(),
                request.clientId());
        try {
            provider = smartPaymentClient.createPaymentLink(
                    authorization,
                    request);
        } catch (FeignException exception) {
            LOGGER.warn(
                    "Smart Payment response error: status={}, body={}",
                    exception.status(),
                    exception.contentUTF8());
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "payment_service_unavailable");
        }
        validateProviderResponse(provider);

        inAdminTransaction(() -> {
            var stored = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new ServiceException(
                            HttpStatus.NOT_FOUND, "payment_charge_not_found"));
            stored.setOrderNsu(provider.orderNsu());
            stored.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(stored);
            return null;
        });

        var output = new CreatePaymentLinkOutput();
        output.url = provider.url();
        output.orderNsu = provider.orderNsu();
        output.status = provider.status();
        output.reused = provider.reused();
        return output;
    }

    public GetPaymentHistoryOutput history() {
        String tenant = requireTenant();
        return inAdminTransaction(() -> {
            var output = new GetPaymentHistoryOutput();
            output.payments = paymentRepository.findAllByTenantOrderByCreatedAtDesc(tenant)
                    .stream()
                    .map(this::toHistoryItem)
                    .toList();
            return output;
        });
    }

    private SubscriptionPaymentEntity preparePayment(
            String tenant,
            String planCode,
            BillingCycle billingCycle) {
        if (planCode == null || planCode.isBlank()
                || billingCycle == null
                || "FREE".equalsIgnoreCase(planCode)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "payment_paid_plan_required");
        }
        var plan = planRepository.findAll().stream()
                .filter(item -> planCode.equalsIgnoreCase(item.getCode()) && item.isActive())
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.NOT_FOUND, "subscription_plan_not_found"));
        if (plan.getPriceMonthly() == null || plan.getPriceMonthly() <= 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "payment_plan_unavailable");
        }
        var discount = discountRepository.findByBillingCycleAndActiveTrue(billingCycle)
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.BAD_REQUEST, "payment_billing_cycle_unavailable"));

        return paymentRepository
                .findFirstByTenantAndPlanCodeAndBillingCycleAndStatusOrderByCreatedAtDesc(
                        tenant, plan.getCode(), billingCycle, PaymentStatus.PENDING)
                .orElseGet(() -> {
                    var monthly = BigDecimal.valueOf(plan.getPriceMonthly());
                    var base = monthly.multiply(BigDecimal.valueOf(discount.getMonths()));
                    var multiplier = BigDecimal.ONE.subtract(
                            BigDecimal.valueOf(discount.getDiscountPercentage()).movePointLeft(2));
                    var total = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                    var created = new SubscriptionPaymentEntity();
                    created.setTenant(tenant);
                    created.setPlanCode(plan.getCode());
                    created.setBillingCycle(billingCycle);
                    created.setMonths(discount.getMonths());
                    created.setBaseAmountCents(base.movePointRight(2).intValueExact());
                    created.setDiscountPercentage(discount.getDiscountPercentage());
                    created.setAmountCents(total.movePointRight(2).intValueExact());
                    created.setStatus(PaymentStatus.PENDING);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return paymentRepository.save(created);
                });
    }

    private PaymentHistoryItemDTO toHistoryItem(SubscriptionPaymentEntity payment) {
        var output = new PaymentHistoryItemDTO();
        output.id = payment.getId();
        output.planCode = payment.getPlanCode();
        output.billingCycle = payment.getBillingCycle();
        output.amountCents = payment.getAmountCents();
        output.status = payment.getStatus();
        output.createdAt = payment.getCreatedAt();
        output.paidAt = payment.getPaidAt();
        output.coverageStartAt = payment.getCoverageStartAt();
        output.coverageEndAt = payment.getCoverageEndAt();
        return output;
    }

    private void validateProviderResponse(SmartPaymentClient.PaymentLinkResponse response) {
        if (response == null || response.url() == null || response.url().isBlank()
                || response.orderNsu() == null || response.orderNsu().isBlank()
                || response.status() == null || response.status().isBlank()) {
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "payment_service_invalid_response");
        }
    }

    private String requireTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(tenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "subscription_tenant_not_identified");
        }
        return tenant;
    }

    private <T> T inAdminTransaction(Supplier<T> operation) {
        String previousTenant = TenantContext.getCurrentTenant();
        try {
            selectTenant(ADMIN_TENANT);
            return transactionTemplate.execute(status -> operation.get());
        } finally {
            entityManager.clear();
            TenantContext.setCurrentTenant(previousTenant);
            if (previousTenant != null && !previousTenant.isBlank()) {
                dbMigration.loadMigrateTenants(previousTenant);
                tenantSchemaInterceptor.switchSchema();
            }
        }
    }

    private void selectTenant(String tenant) {
        dbMigration.loadMigrateTenants(tenant);
        TenantContext.setCurrentTenant(tenant);
        tenantSchemaInterceptor.switchSchema();
    }
}
