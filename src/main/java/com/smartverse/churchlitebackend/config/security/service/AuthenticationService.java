package com.smartverse.churchlitebackend.config.security.service;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.security.Authenticate;
import com.potatotech.authorization.security.UserSupplier;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.security.model.RegisterDTO;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierDTO;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend.repository.userconfirmation.UserConfirmationCustomRepository;
import com.smartverse.churchlitebackend.service.email.EmailService;
import com.smartverse.churchlitebackend_gen.entities.UserConfirmationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    @Autowired
    AuthenticationRepository authenticationRepository;

    @Autowired
    UserConfirmationCustomRepository userConfirmationRepository;

    @Autowired
    Authenticate authenticate;

    @Autowired
    EmailService emailService;

    @Value("${app.frontend.base-url:${FRONTEND_BASE_URL:http://localhost:4200}}")
    String frontendBaseUrl;

    public List<AuthenticatedChurch> login(UserSupplierDTO userSupplierDTO){
        TenantContext.setCurrentTenant("admin");
        var passwordEncoder = new BCryptPasswordEncoder();

        // No login multi-tenant, cada registro precisa validar a senha antes de seu token ser
        // retornado. Se mais de um vínculo usar a mesma senha, todos serão oferecidos para
        // seleção e o primeiro (ordenado por ID) será mantido como token principal.
        var passwordMatches = authenticationRepository
                .findAllByEmailOrderByIdAsc(userSupplierDTO.email())
                .stream()
                .filter(user -> passwordEncoder.matches(userSupplierDTO.password(), user.getPassword()))
                .toList();

        if (passwordMatches.isEmpty()) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED,"User or password invalid");
        }

        var authenticatedChurches = passwordMatches.stream()
                .filter(user -> user.isUserConfirm() && user.isActive())
                .map(user -> new AuthenticatedChurch(
                        user.getId(),
                        user.getName(),
                        user.getTenant(),
                        authenticate.generateToken(setUserSupplier(user))))
                .toList();

        if (authenticatedChurches.isEmpty()) {
            throw new ServiceException(HttpStatus.FORBIDDEN,"account_confirmation_required");
        }

        return authenticatedChurches;
    }

    public record AuthenticatedChurch(UUID userId, String name, String tenant, String accessToken) {}

    public UserSupplier validateToken(String token){
        token = token.replace("Bearer ","");
        return authenticate.isAuthenticated(token);
    }

    private UserSupplier setUserSupplier(UserSupplierEntity userSupplier){
        var usersup =  UserSupplier.builder().build();
        usersup.setId(userSupplier.getId());
        usersup.setName(userSupplier.getName());
        usersup.setTenant(userSupplier.getTenant());
        usersup.setEmail(userSupplier.getEmail());
        usersup.setGroupRoles(Collections.emptyList());
        return usersup;
    }

    @Transactional
    public boolean onRegisterUser(RegisterDTO register){

        if (register.name() == null || register.name().isBlank()
                || register.password() == null || register.password().isBlank()
                || register.email() == null || register.email().isBlank()
                || register.cpf() == null || register.cpf().isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,"Campos com dados inválidos");
        }

        var normalizedEmail = normalizeEmail(register.email());
        var normalizedCpf = normalizeCpf(register.cpf());
        if (!isValidCpf(normalizedCpf)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "invalid_cpf");
        }
        var existingUser = authenticationRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(normalizedEmail).orElse(null);

        if (existingUser != null) {
            if (!existingUser.isUserConfirm() && !existingUser.isActive()) {
                throw new ServiceException(HttpStatus.CONFLICT,"account_confirmation_pending");
            }
            throw new ServiceException(HttpStatus.CONFLICT,"email_already_registered");
        }

        var count = authenticationRepository.countAllBy();
        var user = new UserSupplierEntity();
        user.setName(register.name());
        user.setEmail(normalizedEmail);
        user.setCpf(normalizedCpf);
        var pass = new BCryptPasswordEncoder().encode(register.password());
        user.setPassword(pass);
        user.setUserConfirm(false);
        user.setActive(false);
        user.setTenant(String.format("SMARTVARSE_%s",count));

        user = authenticationRepository.save(user);

        var userConfirmation = new UserConfirmationEntity();

        userConfirmation.setUserId(user.getId());
        userConfirmation.setHash(UUID.randomUUID().toString());
        userConfirmation = userConfirmationRepository.save(userConfirmation);

        sendConfirmationEmail(user, userConfirmation.getHash());

        return true;
    }

    @Transactional
    public boolean resendConfirmation(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }

        var user = authenticationRepository
                .findFirstByEmailIgnoreCaseOrderByIdAsc(normalizeEmail(email))
                .orElse(null);

        if (user == null || user.isUserConfirm() || user.isActive()) {
            return true;
        }

        var confirmation = userConfirmationRepository.findByUserId(user.getId())
                .orElseGet(UserConfirmationEntity::new);
        confirmation.setUserId(user.getId());
        confirmation.setHash(UUID.randomUUID().toString());
        confirmation = userConfirmationRepository.save(confirmation);

        sendConfirmationEmail(user, confirmation.getHash());
        return true;
    }

    private void sendConfirmationEmail(UserSupplierEntity user, String token) {
        var confirmationUrl = frontendBaseUrl.replaceAll("/+$", "") + "/register-church/" + token;
        var emailContent = emailService.loadModel("new-churc")
                .replace("{{name}}", escapeHtml(user.getName()))
                .replace("{{url}}", confirmationUrl);
        emailService.sendEmail(user.getEmail(), "Confirme sua conta no Church Lite", emailContent, token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCpf(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }

        for (int digitPosition = 9; digitPosition <= 10; digitPosition++) {
            int sum = 0;
            for (int index = 0; index < digitPosition; index++) {
                sum += Character.digit(cpf.charAt(index), 10) * (digitPosition + 1 - index);
            }
            int digit = 11 - (sum % 11);
            if (digit >= 10) digit = 0;
            if (digit != Character.digit(cpf.charAt(digitPosition), 10)) return false;
        }
        return true;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
