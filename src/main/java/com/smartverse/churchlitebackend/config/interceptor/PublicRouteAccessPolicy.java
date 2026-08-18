package com.smartverse.churchlitebackend.config.interceptor;

import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicRouteAccessPolicy {
    private static final String ADMIN_TENANT = "admin";

    private static final List<String> ADMIN_PUBLIC_EXACT_ROUTES = List.of(
            "/church-lite/authenticate",
            "/church-lite/register",
            "/church-lite/verifyURL",
            "/church-lite/resendConfirmation"
    );

    private static final List<String> ADMIN_PUBLIC_PREFIXES = List.of(
            "/church-lite/member-access/"
    );

    private static final List<String> INFRASTRUCTURE_PUBLIC_ROUTES = List.of(
            "/church-lite/swagger-ui/",
            "/church-lite/v3/",
            "/church-lite/error"
    );

    private final DBMigration dbMigration;

    public PublicRouteAccessPolicy(DBMigration dbMigration) {
        this.dbMigration = dbMigration;
    }

    public boolean handleIfPublic(String uri) {
        if (startsWithAny(uri, INFRASTRUCTURE_PUBLIC_ROUTES)) {
            return true;
        }
        if (ADMIN_PUBLIC_EXACT_ROUTES.contains(uri) || startsWithAny(uri, ADMIN_PUBLIC_PREFIXES)) {
            TenantContext.setCurrentTenant(ADMIN_TENANT);
            dbMigration.loadMigrateTenants(ADMIN_TENANT);
            return true;
        }
        return false;
    }

    private boolean startsWithAny(String uri, List<String> routes) {
        return routes.stream().anyMatch(uri::startsWith);
    }
}
