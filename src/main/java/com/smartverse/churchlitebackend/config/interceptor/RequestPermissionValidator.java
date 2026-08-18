package com.smartverse.churchlitebackend.config.interceptor;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.config.metadata.PermissionCatalogService;
import com.smartverse.churchlitebackend.service.permissions.PermissionGroupBusinessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestPermissionValidator {
    private final PermissionCatalogService permissionCatalogService;
    private final PermissionGroupBusinessService permissionGroupService;

    public RequestPermissionValidator(PermissionCatalogService permissionCatalogService,
                                      PermissionGroupBusinessService permissionGroupService) {
        this.permissionCatalogService = permissionCatalogService;
        this.permissionGroupService = permissionGroupService;
    }

    public void validate(HttpServletRequest request, UUID userId) {
        var resource = permissionCatalogService.resolveResource(request.getRequestURI(), request.getContextPath());
        var permission = resource == null ? null : permissionCatalogService.resolvePermission(resource, request.getMethod());
        if (resource != null && permission != null && permissionGroupService.isDenied(userId, resource, permission)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "permission_access_denied");
        }
    }
}
