package com.smartverse.churchlitebackend.messaging.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.service.payment.SubscriptionPaymentBusinessService;
import com.smartverse.churchlitebackend_gen.messaging.sub.PaymentConfirmedSub;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedListener extends PaymentConfirmedSub {
    private final ObjectMapper mapper;
    private final SubscriptionPaymentBusinessService service;

    public PaymentConfirmedListener(ObjectMapper m, SubscriptionPaymentBusinessService s) {
        mapper = m;
        service = s;
    }

    protected void onMessage(String message) {
        try {
            service.confirm(mapper.readValue(message, SubscriptionPaymentBusinessService.Event.class));
        } catch (Exception e) {
            throw new IllegalStateException("payment_event_invalid", e);
        }
    }
}
