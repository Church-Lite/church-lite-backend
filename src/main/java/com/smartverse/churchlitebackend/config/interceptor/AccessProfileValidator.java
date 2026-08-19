package com.smartverse.churchlitebackend.config.interceptor;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.config.context.ConfigContextImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Set;
import java.util.UUID;

@Component
public class AccessProfileValidator {
    private static final Logger log = LoggerFactory.getLogger(AccessProfileValidator.class);
    private static final String ACCESS_PROFILE_HEADER = "XAccessProfile";
    private static final String MEMBER_PROFILE = "MEMBER";
    private static final String STAFF_PROFILE = "STAFF";
    private static final String MEMBER_API_SEGMENT = "/member-api/";
    private static final Set<String> MEMBER_STORAGE_ROUTES = Set.of(
            "/requestUpload", "/requestUrl", "/deleteObject");

    private final String adminSchema;
    private final JdbcTemplate jdbcTemplate;

    public AccessProfileValidator(ConfigContextImpl configContext, DataSource dataSource) {
        this.adminSchema = configContext.getDatabase().toUpperCase() + "_ADMIN";
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void validate(HttpServletRequest request, UUID userId) {
        Set<String> profiles;
        try {
            profiles = Set.copyOf(jdbcTemplate.queryForList(
                    "SELECT profile FROM \"" + adminSchema + "\".user_access_profile WHERE user_access_id = ?",
                    String.class, userId));
        } catch (Exception ignored) {
            // Compatibilidade durante rollout, antes da migration de perfis estar disponível.
            return;
        }

        var requestedProfile = resolveRequestedProfile(request, profiles);
        if (!profiles.contains(requestedProfile)) {
            log.warn("access_profile_denied uri={} requestedProfile={} availableProfiles={} userId={} reason=profile_not_allowed",
                    request.getRequestURI(), requestedProfile, profiles, userId);
            throw new ServiceException(HttpStatus.FORBIDDEN, "access_profile_not_allowed");
        }

        var uri = request.getRequestURI();
        if (MEMBER_PROFILE.equals(requestedProfile)
                && !uri.contains(MEMBER_API_SEGMENT)
                && MEMBER_STORAGE_ROUTES.stream().noneMatch(uri::endsWith)) {
            log.warn("access_profile_denied uri={} requestedProfile={} availableProfiles={} userId={} reason=member_on_administrative_route",
                    uri, requestedProfile, profiles, userId);
            throw new ServiceException(HttpStatus.FORBIDDEN, "member_administrative_access_denied");
        }
    }

    private String resolveRequestedProfile(HttpServletRequest request, Set<String> profiles) {
        var requested = request.getHeader(ACCESS_PROFILE_HEADER);
        if (requested == null || requested.isBlank()) {
            return profiles.size() == 1 && profiles.contains(MEMBER_PROFILE) ? MEMBER_PROFILE : STAFF_PROFILE;
        }
        return requested.toUpperCase();
    }
}
