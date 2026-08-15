package com.smartverse.churchlitebackend.service.report;

import com.potatotech.authorization.exception.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class IntegrationConfigurationParser {

    public Map<String, String> parse(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            throw invalidConfiguration();
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (String entry : configuration.split(";")) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw invalidConfiguration();
            }
            String key = entry.substring(0, separator).trim().toUpperCase(Locale.ROOT);
            String value = entry.substring(separator + 1).trim();
            if (key.isBlank() || value.isBlank() || values.putIfAbsent(key, value) != null) {
                throw invalidConfiguration();
            }
        }
        return Map.copyOf(values);
    }

    private ServiceException invalidConfiguration() {
        return new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "integration_configuration_invalid");
    }
}
