package com.smartverse.churchlitebackend.service.email;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.common.FileCommon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class EmailService {

    private final RestClient resend;
    private final String from;

    public EmailService(RestClient.Builder builder,
                        @Value("${resend.api-key:${RESEND_KEY:}}") String resendKey,
                        @Value("${resend.from:${RESEND_FROM:Church Lite <no-reply@smartverse.com.br>}}") String from) {
        this.resend = builder
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + resendKey)
                .build();
        this.from = from;
    }

    public void sendEmail(String to, String subject, String model, String idempotencyKey) {
        try {
            var response = resend.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(new ResendEmailRequest(from, List.of(to), subject, model))
                    .retrieve()
                    .body(ResendEmailResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new ServiceException(HttpStatus.BAD_GATEWAY, "email_delivery_failed");
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServiceException(HttpStatus.BAD_GATEWAY, "email_delivery_failed");
        }
    }

    public String loadModel(String modelName) {
        return FileCommon.loadMod(modelName);
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {}

    private record ResendEmailResponse(String id) {}
}
