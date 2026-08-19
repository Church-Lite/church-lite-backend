package com.smartverse.churchlitebackend.service.memberportal;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.config.security.model.AccessProfile;
import com.smartverse.churchlitebackend.config.security.model.MemberPortalChurchLinkEntity;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend.config.security.repository.MemberPortalChurchLinkRepository;
import com.smartverse.churchlitebackend.controller.memberportal.MemberPortalModels.*;
import com.smartverse.churchlitebackend.repository.userconfirmation.UserConfirmationCustomRepository;
import com.smartverse.churchlitebackend.service.email.EmailService;
import com.smartverse.churchlitebackend.messaging.social.MemberProfileEventDispatcher;
import com.smartverse.churchlitebackend_gen.entities.*;
import com.smartverse.churchlitebackend_gen.enums.Status;
import com.smartverse.churchlitebackend_gen.enums.TypePerson;
import com.smartverse.churchlitebackend_gen.enums.TypePersonChurch;
import com.smartverse.churchlitebackend_gen.repositories.ChurchConfigurationRepository;
import com.smartverse.churchlitebackend_gen.repositories.PersonMemberRepository;
import com.smartverse.churchlitebackend_gen.repositories.PersonRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MemberPortalAccessService {
    private static final String ADMIN_TENANT = "admin";

    private final MemberPortalChurchLinkRepository linkRepository;
    private final AuthenticationRepository authenticationRepository;
    private final UserConfirmationCustomRepository confirmationRepository;
    private final PersonMemberRepository memberRepository;
    private final PersonRepository personRepository;
    private final ChurchConfigurationRepository churchRepository;
    private final TenantSchemaInterceptor schemaInterceptor;
    private final DBMigration dbMigration;
    private final EmailService emailService;
    private final MemberProfileEventDispatcher memberProfileEventDispatcher;
    private final EntityManager entityManager;
    private final String frontendBaseUrl;

    public MemberPortalAccessService(MemberPortalChurchLinkRepository linkRepository,
                                     AuthenticationRepository authenticationRepository,
                                     UserConfirmationCustomRepository confirmationRepository,
                                     PersonMemberRepository memberRepository,
                                     PersonRepository personRepository,
                                     ChurchConfigurationRepository churchRepository,
                                     TenantSchemaInterceptor schemaInterceptor,
                                     DBMigration dbMigration,
                                     EmailService emailService,
                                     MemberProfileEventDispatcher memberProfileEventDispatcher,
                                     EntityManager entityManager,
                                     @Value("${app.frontend.base-url:${FRONTEND_BASE_URL:http://localhost:4200}}") String frontendBaseUrl) {
        this.linkRepository = linkRepository;
        this.authenticationRepository = authenticationRepository;
        this.confirmationRepository = confirmationRepository;
        this.memberRepository = memberRepository;
        this.personRepository = personRepository;
        this.churchRepository = churchRepository;
        this.schemaInterceptor = schemaInterceptor;
        this.dbMigration = dbMigration;
        this.emailService = emailService;
        this.memberProfileEventDispatcher = memberProfileEventDispatcher;
        this.entityManager = entityManager;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public PortalLinkResponse getOrCreateChurchLink() {
        var churchTenant = TenantContext.getCurrentTenant();
        try {
            switchSchema(ADMIN_TENANT);
            var linkId = (UUID) entityManager.createNativeQuery("""
                    INSERT INTO member_portal_church_link (id, tenant, active)
                    VALUES (:id, :tenant, TRUE)
                    ON CONFLICT (tenant) DO UPDATE SET active = TRUE
                    RETURNING id
                    """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("tenant", churchTenant)
                    .getSingleResult();
            return new PortalLinkResponse(linkId, "/member-access/" + linkId);
        } finally {
            switchSchema(churchTenant);
        }
    }

    @Transactional(readOnly = true)
    public RegistrationContext context(UUID churchId, UUID memberId) {
        var tenant = resolveTenant(churchId);
        try {
            switchSchema(tenant);
            var churchName = churchRepository.findAll().stream().findFirst()
                    .map(ChurchConfigurationEntity::getName).orElse("Igreja");
            if (memberId == null) return new RegistrationContext(churchName, null, null, null, false);
            var member = memberRepository.findById(memberId)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "member_portal_member_not_found"));
            var person = member.getPerson();
            var email = person.getPersonalEmail() == null ? null : person.getPersonalEmail().getEmail();
            return new RegistrationContext(churchName, member.getId(), person.getName(), maskEmail(email), true);
        } finally {
            switchSchema(ADMIN_TENANT);
        }
    }

    @Transactional
    public RegistrationResponse register(UUID churchId, UUID memberId, RegistrationRequest request) {
        validate(request);
        var tenant = resolveTenant(churchId);
        var email = request.email().trim().toLowerCase(Locale.ROOT);
        var cpf = normalizeCpf(request.cpf());
        if (!isValidCpf(cpf)) throw error(HttpStatus.BAD_REQUEST, "invalid_cpf");

        PersonMemberEntity member;
        try {
            switchSchema(tenant);
            member = memberId == null ? createMember(request, cpf, email) : requireExistingMember(memberId, email, cpf);
        } finally {
            switchSchema(ADMIN_TENANT);
        }

        var existing = authenticationRepository.findFirstByEmailIgnoreCaseAndTenant(email, tenant).orElse(null);
        var existingAccess = existing != null && existing.isActive() && existing.isUserConfirm();
        var access = existing == null ? new UserSupplierEntity() : existing;
        if (existing == null) {
            access.setName(member.getPerson().getName());
            access.setEmail(email);
            access.setCpf(cpf);
            access.setPhone(normalizeOptional(request.phone()));
            access.setTenant(tenant);
            access.setPassword(new BCryptPasswordEncoder().encode(request.password()));
            access.setActive(false);
            access.setUserConfirm(false);
        }
        var profiles = access.getAccessProfiles() == null ? new HashSet<AccessProfile>() : new HashSet<>(access.getAccessProfiles());
        profiles.add(AccessProfile.MEMBER);
        access.setAccessProfiles(profiles);
        access = authenticationRepository.saveAndFlush(access);

        try {
            switchSchema(tenant);
            var storedMember = memberRepository.findById(member.getId()).orElseThrow();
            storedMember.setAccessUserHash(access.getId());
            memberRepository.saveAndFlush(storedMember);
        } finally {
            switchSchema(ADMIN_TENANT);
        }

        if (!existingAccess) sendConfirmation(access);
        memberProfileEventDispatcher.schedule(access, member);
        return new RegistrationResponse(true, existingAccess);
    }

    @Transactional
    public String memberLink(UUID memberId) {
        var base = getOrCreateChurchLink();
        return base.path() + "/" + memberId;
    }

    @Transactional
    public void sendMemberLink(UUID memberId) {
        var tenant = TenantContext.getCurrentTenant();
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "member_portal_member_not_found"));
        var email = member.getPerson().getPersonalEmail() == null ? null : member.getPerson().getPersonalEmail().getEmail();
        if (email == null || email.isBlank()) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "member_portal_email_required");
        var path = memberLink(memberId);
        var url = frontendBaseUrl.replaceAll("/+$", "") + path;
        var content = emailService.renderModel("member-access-invitation",
                Map.of("name", escapeHtml(member.getPerson().getName()), "url", url));
        emailService.sendEmail(email, "Ative seu acesso ao portal do membro", content,
                "member-link-" + UUID.randomUUID());
    }

    public void scheduleProfileSync(UserSupplierEntity access) {
        if (access.getAccessProfiles() == null || !access.getAccessProfiles().contains(AccessProfile.MEMBER)) return;
        try {
            switchSchema(access.getTenant());
            entityManager.createQuery(
                            "select member from PersonMemberEntity member join fetch member.person where member.accessUserHash = :accessId",
                            PersonMemberEntity.class)
                    .setParameter("accessId", access.getId())
                    .getResultStream()
                    .findFirst()
                    .ifPresent(member -> memberProfileEventDispatcher.schedule(access, member));
        } finally {
            switchSchema(ADMIN_TENANT);
        }
    }

    private PersonMemberEntity createMember(RegistrationRequest request, String cpf, String email) {
        var duplicate = entityManager.createQuery("select count(d) from PersonalDocsEntity d where d.cpf = :cpf", Long.class)
                .setParameter("cpf", cpf).getSingleResult();
        if (duplicate > 0) throw error(HttpStatus.CONFLICT, "member_portal_cpf_already_registered");

        var person = new PersonEntity();
        person.setName(request.name().trim());
        person.setStatus(Status.ACTIVE);
        person.setType(TypePersonChurch.MEMBER);

        var docs = new PersonalDocsEntity();
        docs.setPerson(person);
        docs.setCpf(cpf);
        docs.setTypePerson(TypePerson.NATURAL_PERSON);
        person.setPersonalDocs(docs);

        var personalEmail = new PersonalEmailEntity();
        personalEmail.setPerson(person);
        personalEmail.setEmail(email);
        person.setPersonalEmail(personalEmail);

        if (request.phone() != null && !request.phone().isBlank()) {
            var phone = new PersonalTelphoneEntity();
            phone.setPerson(person);
            phone.setCellPhone(request.phone().trim());
            person.setPersonalTelphone(phone);
        }

        var member = new PersonMemberEntity();
        member.setPerson(person);
        member.setEntryDate(LocalDate.now());
        person.setPersonMember(member);
        personRepository.saveAndFlush(person);
        return member;
    }

    private PersonMemberEntity requireExistingMember(UUID memberId, String email, String cpf) {
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "member_portal_member_not_found"));
        var person = member.getPerson();
        var currentEmail = person.getPersonalEmail() == null ? null : person.getPersonalEmail().getEmail();
        if (currentEmail != null && !currentEmail.isBlank() && !currentEmail.equalsIgnoreCase(email)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "member_portal_email_mismatch");
        }
        var currentCpf = person.getPersonalDocs() == null ? null : normalizeCpf(person.getPersonalDocs().getCpf());
        if (currentCpf != null && !currentCpf.isBlank() && !currentCpf.equals(cpf)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "member_portal_cpf_mismatch");
        }
        if (person.getPersonalEmail() == null) {
            var personalEmail = new PersonalEmailEntity();
            personalEmail.setPerson(person);
            person.setPersonalEmail(personalEmail);
        }
        person.getPersonalEmail().setEmail(email);
        if (person.getPersonalDocs() == null) {
            var docs = new PersonalDocsEntity();
            docs.setPerson(person);
            docs.setTypePerson(TypePerson.NATURAL_PERSON);
            person.setPersonalDocs(docs);
        }
        person.getPersonalDocs().setCpf(cpf);
        personRepository.saveAndFlush(person);
        return member;
    }

    private void sendConfirmation(UserSupplierEntity access) {
        var confirmation = confirmationRepository.findByUserId(access.getId()).orElseGet(UserConfirmationEntity::new);
        confirmation.setUserId(access.getId());
        confirmation.setHash(UUID.randomUUID().toString());
        confirmationRepository.saveAndFlush(confirmation);
        var url = frontendBaseUrl.replaceAll("/+$", "") + "/member-access/confirm/" + confirmation.getHash();
        var content = emailService.renderModel("member-access-confirmation",
                Map.of("name", escapeHtml(access.getName()), "url", url));
        emailService.sendEmail(access.getEmail(), "Confirme seu acesso ao portal do membro", content, confirmation.getHash());
    }

    private String resolveTenant(UUID churchId) {
        switchSchema(ADMIN_TENANT);
        var link = linkRepository.findById(churchId)
                .filter(MemberPortalChurchLinkEntity::isActive)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "member_portal_link_not_found"));
        dbMigration.loadMigrateTenants(link.getTenant());
        return link.getTenant();
    }

    private void validate(RegistrationRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.email()) || isBlank(request.cpf())
                || isBlank(request.password()) || !request.password().equals(request.passwordConfirmation())) {
            throw error(HttpStatus.BAD_REQUEST, "member_portal_invalid_data");
        }
        if (!request.email().contains("@")) throw error(HttpStatus.BAD_REQUEST, "member_portal_invalid_email");
        if (request.password().length() < 6) throw error(HttpStatus.BAD_REQUEST, "member_portal_invalid_password");
    }

    private void switchSchema(String tenant) {
        TenantContext.setCurrentTenant(tenant);
        schemaInterceptor.switchSchema();
    }

    private String normalizeCpf(String cpf) { return cpf == null ? "" : cpf.replaceAll("\\D", ""); }
    private String normalizeOptional(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private ServiceException error(HttpStatus status, String message) { return new ServiceException(status, message); }

    private boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        for (int position = 9; position <= 10; position++) {
            int sum = 0;
            for (int index = 0; index < position; index++) sum += Character.digit(cpf.charAt(index), 10) * (position + 1 - index);
            int digit = 11 - sum % 11;
            if (digit >= 10) digit = 0;
            if (digit != Character.digit(cpf.charAt(position), 10)) return false;
        }
        return true;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return null;
        var parts = email.split("@", 2);
        return parts[0].substring(0, 1) + "***@" + parts[1];
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
