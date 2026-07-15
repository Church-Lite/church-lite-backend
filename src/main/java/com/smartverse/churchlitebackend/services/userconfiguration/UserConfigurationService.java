package com.smartverse.churchlitebackend.services.userconfiguration;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend_gen.CreateChurchUserInput;
import com.smartverse.churchlitebackend_gen.Language;
import com.smartverse.churchlitebackend_gen.Theme;
import com.smartverse.churchlitebackend_gen.UserConfigurationDTO;
import com.smartverse.churchlitebackend_gen.UserConfigurationDTOConverter;
import com.smartverse.churchlitebackend_gen.UserConfigurationEntity;
import com.smartverse.churchlitebackend_gen.UserConfigurationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserConfigurationService {

    private static final String ADMIN_TENANT = "admin";

    private final TenantSchemaInterceptor tenantSchemaInterceptor;
    private final AuthenticationRepository authenticationRepository;
    private final UserConfigurationRepository userConfigurationRepository;
    private final UserConfigurationDTOConverter userConfigurationDTOConverter;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserConfigurationService(
            TenantSchemaInterceptor tenantSchemaInterceptor,
            AuthenticationRepository authenticationRepository,
            UserConfigurationRepository userConfigurationRepository,
            UserConfigurationDTOConverter userConfigurationDTOConverter) {
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
        this.authenticationRepository = authenticationRepository;
        this.userConfigurationRepository = userConfigurationRepository;
        this.userConfigurationDTOConverter = userConfigurationDTOConverter;
    }

    @Transactional
    public UserConfigurationDTO createChurchUser(CreateChurchUserInput input) {
        validateNewUser(input);

        String churchTenant = TenantContext.getCurrentTenant();
        if (churchTenant == null || churchTenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(churchTenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Tenant da igreja não identificado");
        }

        String normalizedEmail = input.email.trim().toLowerCase(Locale.ROOT);
        UserSupplierEntity accessUser;

        try {
            switchSchema(ADMIN_TENANT);

            if (authenticationRepository.existsByEmailAndTenant(normalizedEmail, churchTenant)) {
                throw new ServiceException(HttpStatus.CONFLICT,
                        "Já existe um usuário com este e-mail nesta igreja");
            }

            accessUser = new UserSupplierEntity();
            accessUser.setName(input.name.trim());
            accessUser.setEmail(normalizedEmail);
            accessUser.setPhone(normalizeOptional(input.phone));
            accessUser.setPassword(passwordEncoder.encode(input.password));
            accessUser.setTenant(churchTenant);
            accessUser.setActive(true);
            accessUser.setUserConfirm(true);
            accessUser = authenticationRepository.saveAndFlush(accessUser);

            switchSchema(churchTenant);

            UserConfigurationEntity configuration = new UserConfigurationEntity();
            configuration.setName(accessUser.getName());
            configuration.setEmail(accessUser.getEmail());
            configuration.setPhone(accessUser.getPhone());
            configuration.setHash(accessUser.getId());
            configuration.setLang(Language.PORTUGUESE);
            configuration.setTheme(Theme.LIGHT);
            configuration = userConfigurationRepository.saveAndFlush(configuration);

            return userConfigurationDTOConverter.toDTO(configuration, null);
        } finally {
            switchSchema(churchTenant);
        }
    }

    @Transactional
    public UserConfigurationDTO saveUserConfiguration(UUID hash) {
        String churchTenant = TenantContext.getCurrentTenant();

        try {
            switchSchema(ADMIN_TENANT);
            var accessUser = authenticationRepository.findById(hash)
                    .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

            switchSchema(churchTenant);

            UserConfigurationEntity configuration = new UserConfigurationEntity();
            configuration.setHash(hash);
            configuration.setName(accessUser.getName());
            configuration.setEmail(accessUser.getEmail());
            configuration.setPhone(accessUser.getPhone());
            configuration.setLang(Language.PORTUGUESE);
            configuration.setTheme(Theme.LIGHT);
            configuration = userConfigurationRepository.saveAndFlush(configuration);

            return userConfigurationDTOConverter.toDTO(configuration, null);
        } finally {
            switchSchema(churchTenant);
        }
    }

    @Transactional
    public void updateMaster(UserConfigurationEntity entity) {
        String churchTenant = TenantContext.getCurrentTenant();

        try {
            switchSchema(ADMIN_TENANT);
            authenticationRepository.findById(entity.getHash()).ifPresent(accessUser -> {
                String normalizedEmail = entity.getEmail().trim().toLowerCase(Locale.ROOT);
                if (authenticationRepository.existsByEmailAndTenantAndIdNot(
                        normalizedEmail, accessUser.getTenant(), accessUser.getId())) {
                    throw new ServiceException(HttpStatus.CONFLICT,
                            "Já existe um usuário com este e-mail nesta igreja");
                }
                accessUser.setName(entity.getName());
                accessUser.setEmail(normalizedEmail);
                accessUser.setPhone(normalizeOptional(entity.getPhone()));
                authenticationRepository.saveAndFlush(accessUser);
            });
        } finally {
            switchSchema(churchTenant);
        }
    }

    @Transactional
    public void deleteMaster(UUID hash) {
        String churchTenant = TenantContext.getCurrentTenant();

        try {
            switchSchema(ADMIN_TENANT);
            authenticationRepository.findById(hash)
                    .ifPresent(authenticationRepository::delete);
            authenticationRepository.flush();
        } finally {
            switchSchema(churchTenant);
        }
    }

    private void validateNewUser(CreateChurchUserInput input) {
        if (input == null
                || isBlank(input.name)
                || isBlank(input.email)
                || isBlank(input.password)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Nome, e-mail e senha são obrigatórios");
        }
        if (!input.email.contains("@")) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "E-mail inválido");
        }
        if (input.password.length() < 6) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "A senha deve possuir pelo menos 6 caracteres");
        }
    }

    private void switchSchema(String tenant) {
        TenantContext.setCurrentTenant(tenant);
        tenantSchemaInterceptor.switchSchema();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
