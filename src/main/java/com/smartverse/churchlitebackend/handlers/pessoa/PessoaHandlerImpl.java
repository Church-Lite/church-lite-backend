package com.smartverse.churchlitebackend.handlers.pessoa;

import com.smartverse.churchlitebackend_gen.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PessoaHandlerImpl implements SavePerson {


    @Override
    public ResponseEntity<SavePersonOutput> savePerson(SavePersonInput input) {
        return null;
    }
}
