package com.smartverse.churchlitebackend.service.cash;

import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.dtos.CashDTO;
import com.smartverse.churchlitebackend_gen.enums.SubscriptionResource;
import com.smartverse.churchlitebackend_gen.enums.TypeCash;
import com.smartverse.churchlitebackend_gen.services.CashService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CashBusinessService extends CashService {
    private final SubscriptionService subscriptionService;

    public CashBusinessService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public CashDTO save(CashDTO obj) {
        subscriptionService.requireAvailable(resourceFor(obj.typeCash));
        return super.save(obj);
    }

    @Override
    @Transactional
    public CashDTO update(CashDTO obj, UUID id) {
        var current = repository.findById(id).orElseThrow();
        if (current.getTypeCash() != obj.typeCash) {
            subscriptionService.requireAvailable(resourceFor(obj.typeCash));
        }
        return super.update(obj, id);
    }

    private SubscriptionResource resourceFor(TypeCash type) {
        return type == TypeCash.BANK
                ? SubscriptionResource.BANK_ACCOUNT
                : SubscriptionResource.CASH_ACCOUNT;
    }
}
