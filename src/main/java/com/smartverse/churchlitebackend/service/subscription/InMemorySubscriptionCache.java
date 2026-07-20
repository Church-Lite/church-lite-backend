package com.smartverse.churchlitebackend.service.subscription;

import com.smartverse.churchlitebackend.model.subscription.SubscriptionSnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySubscriptionCache implements SubscriptionCache {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<SubscriptionSnapshot> get(String tenant) {
        var entry = entries.get(tenant);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(tenant, entry);
            return Optional.empty();
        }
        return Optional.of(entry.snapshot());
    }

    @Override
    public void put(String tenant, SubscriptionSnapshot snapshot) {
        entries.put(tenant, new CacheEntry(snapshot, Instant.now().plus(TTL)));
    }

    @Override
    public void evict(String tenant) {
        entries.remove(tenant);
    }

    @Override
    public void evictAll() {
        entries.clear();
    }

    private record CacheEntry(SubscriptionSnapshot snapshot, Instant expiresAt) {
    }
}
