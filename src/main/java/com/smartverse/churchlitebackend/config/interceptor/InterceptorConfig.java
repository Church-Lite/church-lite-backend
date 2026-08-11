package com.smartverse.churchlitebackend.config.interceptor;


import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.security.Authenticate;
import com.potatotech.authorization.tenant.TenantConfiguration;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend.config.metadata.PermissionCatalogService;
import com.smartverse.churchlitebackend.service.permissions.PermissionGroupBusinessService;
import feign.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.UUID;

@Configuration
public class InterceptorConfig extends Authenticate implements HandlerInterceptor, WebMvcConfigurer  {


    @Autowired
    DBMigration dbMigration;
    private final PermissionCatalogService permissionCatalogService;
    private final PermissionGroupBusinessService permissionGroupService;

    public InterceptorConfig(PermissionCatalogService permissionCatalogService, PermissionGroupBusinessService permissionGroupService) {
        this.permissionCatalogService = permissionCatalogService;
        this.permissionGroupService = permissionGroupService;
    }
    private static final String AUTHORIZATION = "Authorization";
    private static final String TENANT = "Xtenant";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        var tenantConfiguration = new TenantConfiguration();

        String uri = request.getRequestURI();

        if(validateDomainsAllowAccess(uri)){
            return true;
        }

        if(isOptions(request)){
            return true;
        }
        var auth = request.getHeader(AUTHORIZATION);
        var tenant = request.getHeader(TENANT);
        UUID userId = null;

        if(!tenantConfiguration.validAnonymous(handler)){
            var user = this.isAuthenticated(auth);
            TenantContext.setCurrentTenant(user.getTenant());
            tenant = user.getTenant();
            userId = user.getId();
            RequestUserContext.set(userId);
        } else {
            if(tenant == null){
                throw new ServiceException(HttpStatus.FORBIDDEN,"tenant is required");
            }
            TenantContext.setCurrentTenant(tenant);
        }

        dbMigration.loadMigrateTenants(tenant);
        if (userId != null) {
            var resource = permissionCatalogService.resolveResource(uri, request.getContextPath());
            var permission = resource == null ? null : permissionCatalogService.resolvePermission(resource, request.getMethod());
            if (resource != null && permission != null && permissionGroupService.isDenied(userId, resource, permission)) {
                throw new ServiceException(HttpStatus.FORBIDDEN, "permission_access_denied");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestUserContext.clear();
    }

    private boolean validateDomainsAllowAccess(String uri) {

        // valida swagger
        if(uri.startsWith("/church-lite/swagger-ui/") || uri.startsWith("/church-lite/v3/")) {
            return true;
        } // valida login e register
        else if(uri.startsWith("/church-lite/authenticate") || uri.startsWith("/church-lite/register")
                || uri.startsWith("/church-lite/verifyURL") || uri.startsWith("/church-lite/resendConfirmation")) {
            TenantContext.setCurrentTenant("admin");
            dbMigration.loadMigrateTenants("admin");
            return true;
        }
        else if(uri.startsWith("/church-lite/error")) {
            return true;
        }
        else {
            return false;
        }
    }


    private boolean isOptions(HttpServletRequest request){
        return Request.HttpMethod.OPTIONS.name().equals(request.getMethod());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(this);
    }
}
