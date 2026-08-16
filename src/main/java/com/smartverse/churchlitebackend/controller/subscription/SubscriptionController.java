package com.smartverse.churchlitebackend.controller.subscription;

import com.smartverse.churchlitebackend.service.storage.MiniIoService;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.endpoints.GetCurrentSubscription;
import com.smartverse.churchlitebackend_gen.endpoints.GetCurrentSubscriptionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController implements GetCurrentSubscription {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final MiniIoService storageService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            MiniIoService storageService) {
        this.subscriptionService = subscriptionService;
        this.storageService = storageService;
    }

    @Override
    public ResponseEntity<GetCurrentSubscriptionOutput> getCurrentSubscription() {
        long storageUsageBytes = getStorageUsageBytesSafely();
        var output = new GetCurrentSubscriptionOutput();
        output.subscription = subscriptionService.getCurrentSubscription(storageUsageBytes);
        return ResponseEntity.ok(output);
    }

    private long getStorageUsageBytesSafely() {
        try {
            return storageService.getStorageUsageBytes();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Nao foi possivel calcular o uso do storage; a assinatura sera retornada com uso zero",
                    exception);
            return 0;
        }
    }
}
