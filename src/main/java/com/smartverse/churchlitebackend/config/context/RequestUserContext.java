package com.smartverse.churchlitebackend.config.context;

import java.util.UUID;

public final class RequestUserContext {
    private static final ThreadLocal<UUID> USER = new ThreadLocal<>();

    private RequestUserContext() {
    }

    public static void set(UUID id) {
        USER.set(id);
    }

    public static UUID getRequired() {
        var id = USER.get();
        if (id == null) throw new IllegalStateException("Authenticated user is unavailable");
        return id;
    }

    public static void clear() {
        USER.remove();
    }
}
