package com.smartverse.churchlitebackend.messaging.social;

import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend_gen.dtos.MemberProfileSyncedEventDTO;
import com.smartverse.churchlitebackend_gen.entities.PersonMemberEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MemberProfileEventDispatcher {
    private final ApplicationEventPublisher events;

    public MemberProfileEventDispatcher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void schedule(UserSupplierEntity access, PersonMemberEntity member) {
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
        events.publishEvent(new MemberProfileSyncRequested(event));
    }

    public record MemberProfileSyncRequested(MemberProfileSyncedEventDTO payload) {}
}
