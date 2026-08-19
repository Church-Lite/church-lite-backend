package com.smartverse.churchlitebackend.model.memberportal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class MemberPortalModels {
    private MemberPortalModels() {
    }

    public record PortalLinkResponse(UUID churchId, String path) {
    }

    public record RegistrationContext(String churchName, UUID memberId, String memberName,
                                      String maskedEmail, boolean existingMember) {
    }

    public record RegistrationRequest(@NotBlank String name,
                                      @NotBlank @Email String email,
                                      @NotBlank String cpf,
                                      String phone,
                                      @NotBlank @Size(min = 6) String password,
                                      @NotBlank String passwordConfirmation) {
    }

    public record RegistrationResponse(boolean accepted, boolean existingAccess) {
    }

    public record LinkedMember(UUID memberId, UUID personId) {
    }
}
