package com.smartverse.churchlitebackend.handlers.userconfiguration;

import com.smartverse.churchlitebackend.repository.userconfiguration.UserConfigurationCustomRepository;
import com.smartverse.churchlitebackend.services.userconfiguration.UserConfigurationService;
import com.smartverse.churchlitebackend_gen.CreateChurchUser;
import com.smartverse.churchlitebackend_gen.CreateChurchUserInput;
import com.smartverse.churchlitebackend_gen.CreateChurchUserOutput;
import com.smartverse.churchlitebackend_gen.GetUser;
import com.smartverse.churchlitebackend_gen.GetUserOutput;
import com.smartverse.churchlitebackend_gen.UserConfigurationDTOConverter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class UserConfigurationCustomHandlerImpl implements GetUser, CreateChurchUser {

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
}
