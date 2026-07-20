package com.smartverse.churchlitebackend.service.person;

import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.dtos.PersonDTO;
import com.smartverse.churchlitebackend_gen.services.PersonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonBusinessService extends PersonService {
    private final SubscriptionService subscriptionService;

    public PersonBusinessService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public PersonDTO save(PersonDTO dto) {
        subscriptionService.requireAvailable(SubscriptionResource.PERSON);
        return super.save(dto);
    }
}
