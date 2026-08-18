package com.smartverse.churchlitebackend.controller.memberportal;

import java.util.UUID;

public final class MemberPortalModels {
    private MemberPortalModels() {}

    public record PortalLinkResponse(UUID churchId, String path) {}
    public record RegistrationContext(String churchName, UUID memberId, String memberName,
                                      String maskedEmail, boolean existingMember) {}
    public record RegistrationRequest(String name, String email, String cpf, String phone,
                                      String password, String passwordConfirmation) {}
    public record RegistrationResponse(boolean accepted, boolean existingAccess) {}
}
