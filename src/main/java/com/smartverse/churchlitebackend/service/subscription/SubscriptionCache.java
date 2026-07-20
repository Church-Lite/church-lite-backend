package com.smartverse.churchlitebackend.service.subscription;

import com.smartverse.churchlitebackend.model.subscription.SubscriptionSnapshot;

import java.util.Optional;

public interface SubscriptionCache {
    Optional<SubscriptionSnapshot> get(String tenant);

    void put(String tenant, SubscriptionSnapshot snapshot);

    void evict(String tenant);

    void evictAll();
}
