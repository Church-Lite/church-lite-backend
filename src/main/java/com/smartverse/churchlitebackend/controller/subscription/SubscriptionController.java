package com.smartverse.churchlitebackend.controller.subscription;

import com.smartverse.churchlitebackend.service.storage.MiniIoService;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.endpoints.GetCurrentSubscription;
import com.smartverse.churchlitebackend_gen.endpoints.GetCurrentSubscriptionOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class SubscriptionController implements GetCurrentSubscription {
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
        var output = new GetCurrentSubscriptionOutput();
        output.subscription = subscriptionService.getCurrentSubscription(
                storageService.getStorageUsageBytes());
        return ResponseEntity.ok(output);
    }
}
