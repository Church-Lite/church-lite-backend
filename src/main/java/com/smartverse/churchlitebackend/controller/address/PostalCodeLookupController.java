package com.smartverse.churchlitebackend.controller.address;

import com.smartverse.churchlitebackend.service.address.PostalCodeLookupService;
import com.smartverse.churchlitebackend_gen.endpoints.LookupPostalCode;
import com.smartverse.churchlitebackend_gen.endpoints.LookupPostalCodeOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostalCodeLookupController implements LookupPostalCode {

    private final PostalCodeLookupService service;

    public PostalCodeLookupController(PostalCodeLookupService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<LookupPostalCodeOutput> lookupPostalCode(String postalCode) {
        return ResponseEntity.ok(service.lookup(postalCode));
    }
}
