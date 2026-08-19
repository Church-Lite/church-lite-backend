package com.smartverse.churchlitebackend.messaging.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend_gen.dtos.SocialProfileImageUpdatedEventDTO;
import com.smartverse.churchlitebackend_gen.messaging.sub.SocialProfileImageUpdatedSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SocialProfileImageUpdatedListener extends SocialProfileImageUpdatedSub {
    private static final Logger log = LoggerFactory.getLogger(SocialProfileImageUpdatedListener.class);
    private final ObjectMapper mapper;
    private final SocialProfileImageProjectionService service;

    public SocialProfileImageUpdatedListener(ObjectMapper mapper, SocialProfileImageProjectionService service) {
        this.mapper = mapper; this.service = service;
    }

    @Override
    protected void onMessage(String message) {
        try {
            var event = mapper.readValue(message, SocialProfileImageUpdatedEventDTO.class);
            log.info("social_profile_image_event_received tenant={} accessId={} eventId={}", event.tenant, event.accessId, event.eventId);
            TenantContext.setCurrentTenant(event.tenant);
            service.applyIfEmpty(event);
        } catch (Exception exception) {
            log.error("social_profile_image_event_failed payload={}", message, exception);
            throw new IllegalStateException("Unable to process social.profile.image.updated", exception);
        } finally {
            TenantContext.setCurrentTenant(null);
        }
    }
}
