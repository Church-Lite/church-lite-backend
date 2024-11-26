package com.smartverse.churchlitebackend.config.security.handler;


import com.potatotech.authorization.security.Authenticate;
import com.potatotech.authorization.stereotype.Anonymous;
import com.smartverse.churchlitebackend.config.security.model.RegisterDTO;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierDTO;
import com.smartverse.churchlitebackend.config.security.service.AuthenticationService;
import com.smartverse.churchlitebackend.repository.userconfiguration.UserConfigurationCustomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Hashtable;

@RestController
@CrossOrigin(origins="*")
@RequestMapping
public class AuthenticationHandlerImpl {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    Authenticate authenticate;

    @Autowired
    UserConfigurationCustomRepository userConfigurationRepository;

    @Anonymous
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody UserSupplierDTO userSupplier){
        var map = new Hashtable<>();
        var token = authenticationService.login(userSupplier);
        var user = authenticate.isAuthenticated(token);

        map.put("accessToken", token);
        map.put("validate", 80000);
        map.put("token", user.getId());
        return ResponseEntity.ok(map);
    }

    @Anonymous
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterDTO register){
        var output = new Hashtable<>();
        output.put("status",HttpStatus.OK);
        output.put("sucess",authenticationService.onRegisterUser(register));
        return ResponseEntity.ok(output);
    }
}
