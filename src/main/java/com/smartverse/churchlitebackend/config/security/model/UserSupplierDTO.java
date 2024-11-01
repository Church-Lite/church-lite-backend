package com.smartverse.churchlitebackend.config.security.model;

import java.util.List;
import java.util.UUID;

public record UserSupplierDTO(UUID id, String name, String email, String password, String tenant,List<String> roles) {}
