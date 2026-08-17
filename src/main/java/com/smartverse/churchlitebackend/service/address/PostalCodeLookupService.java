package com.smartverse.churchlitebackend.service.address;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend.client.address.ViaCepClient;
import com.smartverse.churchlitebackend_gen.converters.CityDTOConverter;
import com.smartverse.churchlitebackend_gen.endpoints.LookupPostalCodeOutput;
import com.smartverse.churchlitebackend_gen.entities.CityEntity;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostalCodeLookupService {

    private final ViaCepClient viaCepClient;
    private final EntityManager entityManager;
    private final CityDTOConverter cityConverter;

    public PostalCodeLookupService(ViaCepClient viaCepClient, EntityManager entityManager, CityDTOConverter cityConverter) {
        this.viaCepClient = viaCepClient;
        this.entityManager = entityManager;
        this.cityConverter = cityConverter;
    }

    @Transactional(readOnly = true)
    public LookupPostalCodeOutput lookup(String rawPostalCode) {
        String postalCode = rawPostalCode == null ? "" : rawPostalCode.replaceAll("\\D", "");
        if (postalCode.length() != 8) throw error(HttpStatus.BAD_REQUEST, "postal_code_invalid");

        final ViaCepClient.ViaCepResponse response;
        try {
            response = viaCepClient.lookup(postalCode);
        } catch (FeignException exception) {
            throw error(HttpStatus.BAD_GATEWAY, "postal_code_provider_unavailable");
        }
        if (response == null || Boolean.TRUE.equals(response.error())) {
            throw error(HttpStatus.NOT_FOUND, "postal_code_not_found");
        }
        if (response.localidade() == null || response.localidade().isBlank()
                || response.uf() == null || response.uf().isBlank()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "postal_code_city_not_registered");
        }

        CityEntity city = entityManager.createQuery("""
                        select c from CityEntity c
                        join c.state s
                        where lower(c.name) = lower(:cityName)
                          and lower(s.abreviation) = lower(:stateCode)
                        """, CityEntity.class)
                .setParameter("cityName", response.localidade().trim())
                .setParameter("stateCode", response.uf().trim())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "postal_code_city_not_registered"));

        var output = new LookupPostalCodeOutput();
        output.postalCode = format(postalCode);
        output.address = text(response.logradouro());
        output.neighborhood = text(response.bairro());
        output.complement = text(response.complemento());
        output.city = cityConverter.toDTO(city, null);
        return output;
    }

    private String text(String value) { return value == null ? "" : value.trim(); }
    private String format(String value) { return value.substring(0, 5) + "-" + value.substring(5); }
    private ServiceException error(HttpStatus status, String key) { return new ServiceException(status, key); }
}
