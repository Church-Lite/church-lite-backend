package com.smartverse.churchlitebackend.messaging.social;

import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend_gen.dtos.TenantSyncedEventDTO;
import com.smartverse.churchlitebackend_gen.messaging.pub.TenantSyncedPub;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class TenantSyncPublisher {
    private static final String ADMIN_TENANT = "admin";

    private final AuthenticationRepository authenticationRepository;
    private final ApplicationEventPublisher applicationEvents;
    private final TenantSyncedPub publisher;

    public TenantSyncPublisher(AuthenticationRepository authenticationRepository,
                               ApplicationEventPublisher applicationEvents,
                               TenantSyncedPub publisher) {
        this.authenticationRepository = authenticationRepository;
        this.applicationEvents = applicationEvents;
        this.publisher = publisher;
    }

    public void schedule(String tenant, boolean active) {
        applicationEvents.publishEvent(new TenantSyncRequested(event(tenant, active)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(TenantSyncRequested requested) {
        publisher.publish(requested.payload());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileExistingTenants() {
        TenantContext.setCurrentTenant(ADMIN_TENANT);
        authenticationRepository.findAll().stream()
                .map(user -> user.getTenant())
                .filter(tenant -> tenant != null && !tenant.isBlank() && !ADMIN_TENANT.equalsIgnoreCase(tenant))
                .distinct()
                .forEach(tenant -> publisher.publish(event(tenant, true)));
    }

    private TenantSyncedEventDTO event(String tenant, boolean active) {
        var event = new TenantSyncedEventDTO();
        event.eventId = UUID.randomUUID();
        event.tenant = tenant;
        event.active = active;
        event.occurredAt = LocalDateTime.now();
        return event;
    }

    public record TenantSyncRequested(TenantSyncedEventDTO payload) {}
}
