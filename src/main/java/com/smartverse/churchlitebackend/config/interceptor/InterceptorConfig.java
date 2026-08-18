package com.smartverse.churchlitebackend.config.interceptor;


import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.security.Authenticate;
import com.potatotech.authorization.tenant.TenantConfiguration;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import feign.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.UUID;

@Configuration
public class InterceptorConfig extends Authenticate implements HandlerInterceptor, WebMvcConfigurer  {


    private final DBMigration dbMigration;
    private final PublicRouteAccessPolicy publicRouteAccessPolicy;
    private final AccessProfileValidator accessProfileValidator;
    private final RequestPermissionValidator requestPermissionValidator;

    public InterceptorConfig(DBMigration dbMigration,
                             PublicRouteAccessPolicy publicRouteAccessPolicy,
                             AccessProfileValidator accessProfileValidator,
                             RequestPermissionValidator requestPermissionValidator) {
        this.dbMigration = dbMigration;
        this.publicRouteAccessPolicy = publicRouteAccessPolicy;
        this.accessProfileValidator = accessProfileValidator;
        this.requestPermissionValidator = requestPermissionValidator;
    }
    private static final String AUTHORIZATION = "Authorization";
    private static final String TENANT = "Xtenant";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        var tenantConfiguration = new TenantConfiguration();

        String uri = request.getRequestURI();

        if(publicRouteAccessPolicy.handleIfPublic(uri)){
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
            accessProfileValidator.validate(request, userId);
        } else {
            if(tenant == null){
                throw new ServiceException(HttpStatus.FORBIDDEN,"tenant is required");
            }
            TenantContext.setCurrentTenant(tenant);
        }

        dbMigration.loadMigrateTenants(tenant);
        if (userId != null) {
            requestPermissionValidator.validate(request, userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestUserContext.clear();
    }

    private boolean isOptions(HttpServletRequest request){
        return Request.HttpMethod.OPTIONS.name().equals(request.getMethod());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(this);
    }
}
