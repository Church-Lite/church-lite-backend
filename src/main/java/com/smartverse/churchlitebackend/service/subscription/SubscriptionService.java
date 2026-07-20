package com.smartverse.churchlitebackend.service.subscription;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionFeature;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend.model.subscription.SubscriptionSnapshot;
import com.smartverse.churchlitebackend_gen.dtos.CurrentSubscriptionDTO;
import com.smartverse.churchlitebackend_gen.dtos.SubscriptionFeatureAvailabilityDTO;
import com.smartverse.churchlitebackend_gen.dtos.SubscriptionResourceUsageDTO;
import com.smartverse.churchlitebackend_gen.entities.SubscriptionPlanEntity;
import com.smartverse.churchlitebackend_gen.entities.TenantSubscriptionEntity;
import com.smartverse.churchlitebackend_gen.enums.CellStatus;
import com.smartverse.churchlitebackend_gen.enums.TypeCash;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPlanFeatureRepository;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPlanLimitRepository;
import com.smartverse.churchlitebackend_gen.repositories.SubscriptionPlanRepository;
import com.smartverse.churchlitebackend_gen.repositories.TenantSubscriptionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Locale;

@Service
public class SubscriptionService {
    private static final String FREE_PLAN = "FREE";
    private static final String ADMIN_TENANT = "admin";

    private final SubscriptionCache cache;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPlanLimitRepository limitRepository;
    private final SubscriptionPlanFeatureRepository featureRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final EntityManager entityManager;
    private final DBMigration dbMigration;
    private final TenantSchemaInterceptor tenantSchemaInterceptor;

    public SubscriptionService(
            SubscriptionCache cache,
            SubscriptionPlanRepository planRepository,
            SubscriptionPlanLimitRepository limitRepository,
            SubscriptionPlanFeatureRepository featureRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            EntityManager entityManager,
            DBMigration dbMigration,
            TenantSchemaInterceptor tenantSchemaInterceptor) {
        this.cache = cache;
        this.planRepository = planRepository;
        this.limitRepository = limitRepository;
        this.featureRepository = featureRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.entityManager = entityManager;
        this.dbMigration = dbMigration;
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
    }

    @Transactional
    public SubscriptionSnapshot getCurrentSnapshot() {
        String tenant = requireTenant();
        return cache.get(tenant).orElseGet(() -> {
            var snapshot = loadSnapshot(tenant);
            cache.put(tenant, snapshot);
            return snapshot;
        });
    }

    @Transactional
    public CurrentSubscriptionDTO getCurrentSubscription(long storageBytes) {
        var snapshot = getCurrentSnapshot();
        var output = new CurrentSubscriptionDTO();
        output.planCode = snapshot.planCode();
        output.planName = snapshot.planName();
        output.priceMonthly = snapshot.priceMonthly();
        output.status = snapshot.status();
        output.currentPeriodEndsAt = snapshot.currentPeriodEndsAt();
        output.gracePeriodEndsAt = snapshot.gracePeriodEndsAt();
        output.resources = snapshot.limits().entrySet().stream()
                .map(entry -> toUsage(entry.getKey(), entry.getValue(), usage(entry.getKey(), storageBytes)))
                .toList();
        output.features = snapshot.features().entrySet().stream()
                .map(entry -> {
                    var feature = new SubscriptionFeatureAvailabilityDTO();
                    feature.feature = entry.getKey().name();
                    feature.enabled = entry.getValue();
                    return feature;
                })
                .toList();
        return output;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requireAvailable(SubscriptionResource resource) {
        requireAvailable(resource, usage(resource, 0));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requireAvailable(SubscriptionResource resource, long currentUsage) {
        var snapshot = getCurrentSnapshot();
        requireOperational(snapshot);
        var limit = snapshot.limits().get(resource);
        if (limit != null && limit.value() != null && currentUsage >= limit.value()) {
            throw new ServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "subscription_limit_reached_" + resource.name().toLowerCase(Locale.ROOT));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requireFeature(SubscriptionFeature feature) {
        var snapshot = getCurrentSnapshot();
        requireOperational(snapshot);
        if (!snapshot.features().getOrDefault(feature, false)) {
            throw new ServiceException(
                    HttpStatus.FORBIDDEN,
                    "subscription_feature_unavailable_" + feature.name().toLowerCase(Locale.ROOT));
        }
    }

    private SubscriptionSnapshot loadSnapshot(String tenant) {
        var subscription = tenantSubscriptionRepository.findAll().stream()
                .filter(item -> tenant.equalsIgnoreCase(item.getTenant()))
                .findFirst()
                .orElseGet(() -> createFreeSubscription(tenant));
        mirrorAdministrativeSubscription(subscription);
        var plan = subscription.getPlan();

        var limits = new EnumMap<SubscriptionResource, SubscriptionSnapshot.Limit>(SubscriptionResource.class);
        limitRepository.findAll().stream()
                .filter(item -> item.getPlan().getId().equals(plan.getId()))
                .forEach(item -> parseResource(item.getResource()).ifPresent(resource ->
                        limits.put(resource, new SubscriptionSnapshot.Limit(
                                item.getLimitValue(), item.getWarningPercentage()))));

        var features = new EnumMap<SubscriptionFeature, Boolean>(SubscriptionFeature.class);
        featureRepository.findAll().stream()
                .filter(item -> item.getPlan().getId().equals(plan.getId()))
                .forEach(item -> parseFeature(item.getFeature()).ifPresent(feature ->
                        features.put(feature, item.isEnabled())));

        return new SubscriptionSnapshot(
                tenant,
                plan.getCode(),
                plan.getName(),
                subscription.getPriceMonthly(),
                subscription.getStatus(),
                subscription.getCurrentPeriodEndsAt(),
                subscription.getGracePeriodEndsAt(),
                limits,
                features);
    }

    private TenantSubscriptionEntity createFreeSubscription(String tenant) {
        SubscriptionPlanEntity freePlan = planRepository.findAll().stream()
                .filter(item -> FREE_PLAN.equalsIgnoreCase(item.getCode()) && item.isActive())
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "subscription_free_plan_not_configured"));

        var subscription = new TenantSubscriptionEntity();
        subscription.setTenant(tenant);
        subscription.setPlan(freePlan);
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setPriceMonthly(freePlan.getPriceMonthly());
        return tenantSubscriptionRepository.save(subscription);
    }

    private void mirrorAdministrativeSubscription(TenantSubscriptionEntity subscription) {
        String churchTenant = TenantContext.getCurrentTenant();
        String tenant = subscription.getTenant();
        String planCode = subscription.getPlan().getCode();
        String status = subscription.getStatus();
        LocalDateTime startedAt = subscription.getStartedAt();
        LocalDateTime currentPeriodEndsAt = subscription.getCurrentPeriodEndsAt();
        LocalDateTime gracePeriodEndsAt = subscription.getGracePeriodEndsAt();
        Double priceMonthly = subscription.getPriceMonthly();
        LocalDateTime priceLockedUntil = subscription.getPriceLockedUntil();

        try {
            switchTenant(ADMIN_TENANT);
            var adminPlan = planRepository.findAll().stream()
                    .filter(item -> planCode.equalsIgnoreCase(item.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new ServiceException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "subscription_admin_plan_not_configured"));
            var administrativeSubscription = tenantSubscriptionRepository.findAll().stream()
                    .filter(item -> tenant.equalsIgnoreCase(item.getTenant()))
                    .findFirst()
                    .orElseGet(TenantSubscriptionEntity::new);
            administrativeSubscription.setTenant(tenant);
            administrativeSubscription.setPlan(adminPlan);
            administrativeSubscription.setStatus(status);
            administrativeSubscription.setStartedAt(startedAt);
            administrativeSubscription.setCurrentPeriodEndsAt(currentPeriodEndsAt);
            administrativeSubscription.setGracePeriodEndsAt(gracePeriodEndsAt);
            administrativeSubscription.setPriceMonthly(priceMonthly);
            administrativeSubscription.setPriceLockedUntil(priceLockedUntil);
            tenantSubscriptionRepository.save(administrativeSubscription);
        } finally {
            switchTenant(churchTenant);
        }
    }

    private void switchTenant(String tenant) {
        entityManager.flush();
        entityManager.clear();
        dbMigration.loadMigrateTenants(tenant);
        TenantContext.setCurrentTenant(tenant);
        tenantSchemaInterceptor.switchSchema();
    }

    private long usage(SubscriptionResource resource, long storageBytes) {
        return switch (resource) {
            case PERSON -> count("select count(e) from PersonEntity e");
            case ADMIN_USER -> count("select count(e) from UserConfigurationEntity e");
            case ACTIVE_CELL -> entityManager.createQuery(
                            "select count(e) from CellEntity e where e.status = :status", Long.class)
                    .setParameter("status", CellStatus.ACTIVE)
                    .getSingleResult();
            case CASH_ACCOUNT -> countCash(TypeCash.CASH);
            case BANK_ACCOUNT -> countCash(TypeCash.BANK);
            case ACTIVE_PERMISSION_GROUP -> count(
                    "select count(e) from PermissionGroupEntity e where e.active = true");
            case STORAGE_BYTES -> Math.max(storageBytes, 0);
        };
    }

    private long count(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private long countCash(TypeCash type) {
        return entityManager.createQuery(
                        "select count(e) from CashEntity e where e.typeCash = :type", Long.class)
                .setParameter("type", type)
                .getSingleResult();
    }

    private SubscriptionResourceUsageDTO toUsage(
            SubscriptionResource resource,
            SubscriptionSnapshot.Limit limit,
            long used) {
        var output = new SubscriptionResourceUsageDTO();
        output.resource = resource.name();
        output.used = used;
        output.limitValue = limit.value();
        output.warningPercentage = limit.warningPercentage();
        if (limit.value() == null) {
            output.remaining = null;
            output.percentage = 0;
            output.allowed = true;
        } else {
            output.remaining = Math.max(limit.value() - used, 0);
            output.percentage = limit.value() == 0
                    ? 100
                    : (int) Math.min((used * 100) / limit.value(), 100);
            output.allowed = used < limit.value();
        }
        return output;
    }

    private void requireOperational(SubscriptionSnapshot snapshot) {
        if ("SUSPENDED".equalsIgnoreCase(snapshot.status())
                || "CANCELLED".equalsIgnoreCase(snapshot.status())) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "subscription_inactive");
        }
    }

    private String requireTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(tenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "subscription_tenant_not_identified");
        }
        return tenant;
    }

    private java.util.Optional<SubscriptionResource> parseResource(String value) {
        try {
            return java.util.Optional.of(SubscriptionResource.valueOf(value));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<SubscriptionFeature> parseFeature(String value) {
        try {
            return java.util.Optional.of(SubscriptionFeature.valueOf(value));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return java.util.Optional.empty();
        }
    }
}
