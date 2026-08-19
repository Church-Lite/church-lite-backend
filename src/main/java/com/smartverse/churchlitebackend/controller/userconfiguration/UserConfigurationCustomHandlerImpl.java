package com.smartverse.churchlitebackend.controller.userconfiguration;

import com.smartverse.churchlitebackend.repository.userconfiguration.UserConfigurationCustomRepository;
import com.smartverse.churchlitebackend.service.userconfiguration.UserConfigurationService;
import com.smartverse.churchlitebackend_gen.converters.UserConfigurationDTOConverter;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserConfigurationCustomHandlerImpl implements GetUser, CreateChurchUser, PromoteMemberPortalUser, DeleteChurchUser {

    private final UserConfigurationCustomRepository userConfigurationRepository;
    private final UserConfigurationDTOConverter userConfigurationDTOConverter;
    private final UserConfigurationService userConfigurationService;

    public UserConfigurationCustomHandlerImpl(
            UserConfigurationCustomRepository userConfigurationRepository,
            UserConfigurationDTOConverter userConfigurationDTOConverter,
            UserConfigurationService userConfigurationService) {
        this.userConfigurationRepository = userConfigurationRepository;
        this.userConfigurationDTOConverter = userConfigurationDTOConverter;
        this.userConfigurationService = userConfigurationService;
    }

    @Override
    public ResponseEntity<GetUserOutput> getUser(UUID hash) {
        var output = new GetUserOutput();
        var user = userConfigurationRepository.findByHash(hash);

        output.output = user
                .map(item -> userConfigurationDTOConverter.toDTO(item, null))
                .orElseGet(() -> userConfigurationService.saveUserConfiguration(hash));

        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<CreateChurchUserOutput> createChurchUser(CreateChurchUserInput input) {
        var output = new CreateChurchUserOutput();
        output.user = userConfigurationService.createChurchUser(input);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<PromoteMemberPortalUserOutput> promoteMemberPortalUser(PromoteMemberPortalUserInput input) {
        var output = new PromoteMemberPortalUserOutput();
        output.user = userConfigurationService.promoteMember(input.memberId);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<DeleteChurchUserOutput> deleteChurchUser(DeleteChurchUserInput input) {
        userConfigurationService.deleteChurchUser(input.userConfigurationId);
        var output = new DeleteChurchUserOutput();
        output.deleted = true;
        return ResponseEntity.ok(output);
    }
}
