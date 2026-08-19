package com.smartverse.churchlitebackend.messaging.social;

import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.service.notification.NotificationBusinessService;
import com.smartverse.churchlitebackend_gen.dtos.SocialProfileImageUpdatedEventDTO;
import com.smartverse.churchlitebackend_gen.enums.NotificationType;
import com.smartverse.churchlitebackend_gen.repositories.PersonMemberRepository;
import com.smartverse.churchlitebackend_gen.repositories.PersonRepository;
import com.smartverse.churchlitebackend_gen.repositories.UserConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialProfileImageProjectionService {
    private static final Logger log = LoggerFactory.getLogger(SocialProfileImageProjectionService.class);
    private final DBMigration migration;
    private final PersonMemberRepository members;
    private final PersonRepository people;
    private final UserConfigurationRepository administrativeUsers;
    private final NotificationBusinessService notifications;

    public SocialProfileImageProjectionService(DBMigration migration, PersonMemberRepository members, PersonRepository people,
                                               UserConfigurationRepository administrativeUsers,
                                               NotificationBusinessService notifications) {
        this.migration = migration; this.members = members; this.people = people;
        this.administrativeUsers = administrativeUsers; this.notifications = notifications;
    }

    @Transactional
    public void applyIfEmpty(SocialProfileImageUpdatedEventDTO event) {
        validate(event);
        migration.loadMigrateTenants(event.tenant);
        var member = members.findById(event.memberId).orElse(null);
        if (member == null || !event.accessId.equals(member.getAccessUserHash()) || !event.personId.equals(member.getPerson().getId())) {
            log.warn("social_profile_image_event_ignored tenant={} accessId={} eventId={} reason=member_identity_mismatch", event.tenant, event.accessId, event.eventId);
            return;
        }
        var person = member.getPerson();
        if (person.getImage() != null && !person.getImage().isBlank()) {
            log.info("social_profile_image_event_ignored tenant={} accessId={} eventId={} reason=image_already_present", event.tenant, event.accessId, event.eventId);
            return;
        }
        person.setImage(event.image.trim());
        people.save(person);
        log.info("social_profile_image_event_applied tenant={} accessId={} personId={} eventId={}", event.tenant, event.accessId, person.getId(), event.eventId);
        var recipients = administrativeUsers.findAll();
        log.info("social_profile_image_notification_started tenant={} memberId={} eventId={} recipientCount={}", event.tenant, member.getId(), event.eventId, recipients.size());
        recipients.forEach(recipient -> notifications.create(
                recipient,
                NotificationType.MEMBER_PROFILE_IMAGE_UPDATED,
                "Foto de membro atualizada",
                person.getName() + " atualizou a imagem do perfil pelo Portal do Membro.",
                "MEMBER",
                member.getId(),
                "/home",
                "social-profile-image:" + event.eventId + ":" + recipient.getId()));
        log.info("social_profile_image_notification_completed tenant={} memberId={} eventId={} recipientCount={}", event.tenant, member.getId(), event.eventId, recipients.size());
    }

    private void validate(SocialProfileImageUpdatedEventDTO event) {
        if (event == null || event.eventId == null || event.accessId == null || event.memberId == null || event.personId == null
                || event.tenant == null || event.tenant.isBlank() || event.image == null || event.image.isBlank() || event.occurredAt == null) {
            throw new IllegalArgumentException("Evento social.profile.image.updated invalido");
        }
    }
}
