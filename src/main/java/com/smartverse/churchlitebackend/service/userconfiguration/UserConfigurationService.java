package com.smartverse.churchlitebackend.service.userconfiguration;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.model.AccessProfile;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;

import com.smartverse.churchlitebackend_gen.converters.UserConfigurationDTOConverter;
import com.smartverse.churchlitebackend_gen.dtos.UserConfigurationDTO;
import com.smartverse.churchlitebackend_gen.endpoints.CreateChurchUserInput;
import com.smartverse.churchlitebackend_gen.entities.UserConfigurationEntity;
import com.smartverse.churchlitebackend_gen.enums.Language;
import com.smartverse.churchlitebackend_gen.enums.Theme;
import com.smartverse.churchlitebackend.repository.userconfiguration.UserConfigurationCustomRepository;
import com.smartverse.churchlitebackend.repository.memberportal.MemberDashboardRepository;
import com.smartverse.churchlitebackend_gen.entities.PersonMemberEntity;
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
    private final UserConfigurationCustomRepository userConfigurationRepository;
    private final UserConfigurationDTOConverter userConfigurationDTOConverter;
    private final SubscriptionService subscriptionService;
    private final MemberDashboardRepository memberDashboardRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserConfigurationService(
            TenantSchemaInterceptor tenantSchemaInterceptor,
            AuthenticationRepository authenticationRepository,
            UserConfigurationCustomRepository userConfigurationRepository,
            UserConfigurationDTOConverter userConfigurationDTOConverter,
            SubscriptionService subscriptionService,
            MemberDashboardRepository memberDashboardRepository) {
        this.tenantSchemaInterceptor = tenantSchemaInterceptor;
        this.authenticationRepository = authenticationRepository;
        this.userConfigurationRepository = userConfigurationRepository;
        this.userConfigurationDTOConverter = userConfigurationDTOConverter;
        this.subscriptionService = subscriptionService;
        this.memberDashboardRepository = memberDashboardRepository;
    }

    @Transactional
    public UserConfigurationDTO createChurchUser(CreateChurchUserInput input) {
        validateNewUser(input);

        String churchTenant = TenantContext.getCurrentTenant();
        if (churchTenant == null || churchTenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(churchTenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Tenant da igreja não identificado");
        }
        subscriptionService.requireAvailable(SubscriptionResource.ADMIN_USER);

        String normalizedEmail = input.email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = normalizeCpf(input.cpf);
        if (normalizedCpf.length() != 11) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "CPF inválido");
        }
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
            accessUser.setCpf(normalizedCpf);
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
            configuration.setCpf(accessUser.getCpf());
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
    public UserConfigurationDTO promoteMember(UUID memberId) {
        String churchTenant = TenantContext.getCurrentTenant();
        if (churchTenant == null || churchTenant.isBlank() || ADMIN_TENANT.equalsIgnoreCase(churchTenant)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Tenant da igreja não identificado");
        }

        UUID accessId;
        try {
            switchSchema(churchTenant);
            PersonMemberEntity member = memberDashboardRepository.findById(memberId)
                    .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
            accessId = member.getAccessUserHash();
            if (accessId == null) {
                throw new ServiceException(HttpStatus.CONFLICT, "Este membro ainda não possui acesso ao portal");
            }

            switchSchema(ADMIN_TENANT);
            UserSupplierEntity accessUser = authenticationRepository.findById(accessId)
                    .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Usuário de acesso não encontrado"));
            if (!churchTenant.equals(accessUser.getTenant())) {
                throw new ServiceException(HttpStatus.CONFLICT, "O usuário não pertence a esta igreja");
            }
            if (!sameIdentity(member, accessUser)) {
                throw new ServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "Os dados do membro e do usuário não conferem");
            }
            var profiles = accessUser.getAccessProfiles() == null ? new java.util.HashSet<AccessProfile>()
                    : new java.util.HashSet<>(accessUser.getAccessProfiles());
            profiles.add(AccessProfile.STAFF);
            accessUser.setAccessProfiles(profiles);
            accessUser.setActive(true);
            accessUser.setUserConfirm(true);
            authenticationRepository.saveAndFlush(accessUser);

            switchSchema(churchTenant);
            UserConfigurationEntity configuration = userConfigurationRepository.findByHash(accessId)
                    .orElseGet(UserConfigurationEntity::new);
            configuration.setHash(accessId);
            configuration.setName(accessUser.getName());
            configuration.setEmail(accessUser.getEmail());
            configuration.setPhone(accessUser.getPhone());
            configuration.setCpf(accessUser.getCpf());
            if (configuration.getLang() == null) configuration.setLang(Language.PORTUGUESE);
            if (configuration.getTheme() == null) configuration.setTheme(Theme.LIGHT);
            configuration = userConfigurationRepository.saveAndFlush(configuration);
            return userConfigurationDTOConverter.toDTO(configuration, null);
        } finally {
            switchSchema(churchTenant);
        }
    }

    @Transactional
    public void deleteChurchUser(UUID userConfigurationId) {
        String churchTenant = TenantContext.getCurrentTenant();
        try {
            switchSchema(churchTenant);
            UserConfigurationEntity configuration = userConfigurationRepository.findById(userConfigurationId)
                    .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
            UUID accessId = configuration.getHash();
            switchSchema(ADMIN_TENANT);
            UserSupplierEntity accessUser = authenticationRepository.findById(accessId).orElse(null);
            if (accessUser != null) {
                var profiles = accessUser.getAccessProfiles() == null ? new java.util.HashSet<AccessProfile>()
                        : new java.util.HashSet<>(accessUser.getAccessProfiles());
                profiles.remove(AccessProfile.STAFF);
                if (profiles.contains(AccessProfile.MEMBER)) {
                    accessUser.setAccessProfiles(profiles);
                    accessUser.setActive(true);
                    accessUser.setUserConfirm(true);
                    authenticationRepository.saveAndFlush(accessUser);
                } else {
                    authenticationRepository.delete(accessUser);
                    authenticationRepository.flush();
                }
            }
            switchSchema(churchTenant);
            userConfigurationRepository.deleteById(userConfigurationId);
            userConfigurationRepository.flush();
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
            configuration.setCpf(accessUser.getCpf());
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
                accessUser.setCpf(normalizeCpf(entity.getCpf()));
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

    private String normalizeCpf(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean sameIdentity(PersonMemberEntity member, UserSupplierEntity access) {
        String memberEmail = member.getPerson().getPersonalEmail() == null ? "" : member.getPerson().getPersonalEmail().getEmail();
        String memberPhone = member.getPerson().getPersonalTelphone() == null ? "" : member.getPerson().getPersonalTelphone().getCellPhone();
        String memberCpf = member.getPerson().getPersonalDocs() == null ? "" : member.getPerson().getPersonalDocs().getCpf();
        String accessEmail = access.getEmail() == null ? "" : access.getEmail().trim();
        String accessPhone = normalizePhone(access.getPhone());
        String accessCpf = normalizeCpf(access.getCpf());
        return !memberEmail.isBlank() && !memberPhone.isBlank() && !memberCpf.isBlank()
                && !accessEmail.isBlank() && !accessPhone.isBlank() && !accessCpf.isBlank()
                && memberEmail.trim().equalsIgnoreCase(accessEmail)
                && normalizePhone(memberPhone).equals(accessPhone)
                && normalizeCpf(memberCpf).equals(accessCpf);
    }
}
