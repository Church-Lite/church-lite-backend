package com.smartverse.churchlitebackend.handlers.pessoa;

import com.smartverse.churchlitebackend_gen.SavePerson;
import com.smartverse.churchlitebackend_gen.SavePersonInput;
import com.smartverse.churchlitebackend_gen.SavePersonOutput;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PessoaHandlerImpl implements SavePerson {

    @Override
    public SavePersonOutput savePerson(SavePersonInput input) {
        return null;
    }
}
