package com.smartverse.churchlitebackend.client.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "viaCep", url = "${via-cep.base-url}")
public interface ViaCepClient {

    @GetMapping("/ws/{postalCode}/json/")
    ViaCepResponse lookup(@PathVariable("postalCode") String postalCode);

    record ViaCepResponse(
            String cep,
            String logradouro,
            String complemento,
            String bairro,
            String localidade,
            String uf,
            String ibge,
            @JsonProperty("erro") Boolean error) {
    }
}
