package com.smartverse.churchlitebackend.handlers.pessoa;

import com.smartverse.churchlitebackend_gen.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PessoaHandlerImpl extends PersonHandler implements SavePerson {


    @Override
    public SavePersonOutput savePerson(SavePersonInput input) {
        return null;
    }

    @Override
    public PersonDTO save(PersonDTO obj) {
        var entity = dtoConverter.toEntity(obj);
        entityManager.persist(entity);
        return dtoConverter.toDTO(entity);
    }
}
