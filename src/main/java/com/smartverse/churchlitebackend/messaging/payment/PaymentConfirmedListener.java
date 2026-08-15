package com.smartverse.churchlitebackend.messaging.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend.service.payment.PaymentConfirmationService;
import com.smartverse.churchlitebackend_gen.dtos.PaymentConfirmedEventDTO;
import com.smartverse.churchlitebackend_gen.messaging.sub.PaymentConfirmedSub;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedListener extends PaymentConfirmedSub {
    private final ObjectMapper objectMapper;
    private final PaymentConfirmationService confirmationService;

    public PaymentConfirmedListener(
            ObjectMapper objectMapper,
            PaymentConfirmationService confirmationService) {
        this.objectMapper = objectMapper;
        this.confirmationService = confirmationService;
    }

    @Override
    protected void onMessage(String message) {
        try {
            confirmationService.receive(
                    objectMapper.readValue(message, PaymentConfirmedEventDTO.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Evento de pagamento confirmado possui JSON invalido", exception);
        }
    }
}
