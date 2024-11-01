package com.smartverse.churchlitebackend.config.security.handler;


import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.stereotype.Anonymous;
import com.smartverse.churchlitebackend.config.security.model.RegisterDTO;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierDTO;
import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import com.smartverse.churchlitebackend.config.security.repository.AuthenticationRepository;
import com.smartverse.churchlitebackend.config.security.service.AuthenticationService;
import com.smartverse.churchlitebackend_gen.ResponseData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Hashtable;

@RestController
@CrossOrigin(origins="*")
@RequestMapping
public class AuthenticationHandlerImpl {

    @Autowired
    AuthenticationService authenticationService;

    @Anonymous
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody UserSupplierDTO userSupplier){
        var map = new Hashtable<>();
        map.put("accessToken", authenticationService.login(userSupplier));
        map.put("validate", 80000);
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
