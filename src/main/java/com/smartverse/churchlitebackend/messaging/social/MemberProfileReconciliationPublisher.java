package com.smartverse.churchlitebackend.messaging.social;

import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.config.security.model.AccessProfile;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend_gen.dtos.MemberProfileSyncedEventDTO;
import com.smartverse.churchlitebackend_gen.entities.PersonMemberEntity;
import com.smartverse.churchlitebackend_gen.messaging.pub.MemberProfileSyncedPub;
import com.smartverse.churchlitebackend_gen.repositories.PersonMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class MemberProfileReconciliationPublisher {
    private static final Logger log = LoggerFactory.getLogger(MemberProfileReconciliationPublisher.class);
    private static final String ADMIN_TENANT = "admin";

    private final AuthenticationRepository authenticationRepository;
    private final PersonMemberRepository memberRepository;
    private final DBMigration migration;
    private final MemberProfileSyncedPub publisher;
    private final TransactionTemplate transactions;

    public MemberProfileReconciliationPublisher(AuthenticationRepository authenticationRepository,
                                                PersonMemberRepository memberRepository,
                                                DBMigration migration,
                                                MemberProfileSyncedPub publisher,
                                                PlatformTransactionManager transactionManager) {
        this.authenticationRepository = authenticationRepository;
        this.memberRepository = memberRepository;
        this.migration = migration;
        this.publisher = publisher;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileExistingMembers() {
        var accesses = loadMemberAccesses();
        log.info("member_profile_reconciliation_started accessCount={}", accesses.size());

        var published = 0;
        for (var access : accesses) {
            var member = loadMember(access);
            if (member == null) {
                log.warn("member_profile_reconciliation_skipped tenant={} accessId={} reason=member_link_not_found",
                        access.getTenant(), access.getId());
                continue;
            }
            publisher.publish(event(access, member));
            published++;
            log.info("member_profile_reconciliation_published tenant={} accessId={} memberId={}",
                    access.getTenant(), access.getId(), member.getId());
        }

        TenantContext.setCurrentTenant(null);
        log.info("member_profile_reconciliation_completed accessCount={} published={}", accesses.size(), published);
    }

    private List<UserSupplierEntity> loadMemberAccesses() {
        TenantContext.setCurrentTenant(ADMIN_TENANT);
        return transactions.execute(status -> authenticationRepository.findAll().stream()
                .filter(access -> access.getTenant() != null && !access.getTenant().isBlank())
                .filter(access -> access.getAccessProfiles() != null
                        && access.getAccessProfiles().contains(AccessProfile.MEMBER))
                .toList());
    }

    private PersonMemberEntity loadMember(UserSupplierEntity access) {
        migration.loadMigrateTenants(access.getTenant());
        TenantContext.setCurrentTenant(access.getTenant());
        return transactions.execute(status -> memberRepository.findAll().stream()
                .filter(member -> access.getId().equals(member.getAccessUserHash()))
                .findFirst()
                .orElse(null));
    }

    private MemberProfileSyncedEventDTO event(UserSupplierEntity access, PersonMemberEntity member) {
        var person = member.getPerson();
        var event = new MemberProfileSyncedEventDTO();
        event.eventId = UUID.randomUUID();
        event.accessId = access.getId();
        event.memberId = member.getId();
        event.personId = person.getId();
        event.tenant = access.getTenant();
        event.name = person.getName();
        event.email = access.getEmail();
        event.phone = access.getPhone();
        event.image = person.getImage();
        event.active = access.isActive() && access.isUserConfirm();
        event.occurredAt = LocalDateTime.now();
        return event;
    }
}
