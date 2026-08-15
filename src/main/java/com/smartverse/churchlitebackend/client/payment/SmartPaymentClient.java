package com.smartverse.churchlitebackend.client.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "smartPayment", url = "${smart-payment.base-url}")
public interface SmartPaymentClient {

    @PostMapping("/paymentLink")
    PaymentLinkResponse createPaymentLink(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PaymentLinkRequest request);

    record PaymentLinkRequest(
            String service,
            int value,
            @JsonProperty("client_id") UUID clientId) {
    }

    record PaymentLinkResponse(
            String url,
            @JsonProperty("order_nsu") String orderNsu,
            String status,
            boolean reused) {
    }
}
