package com.smartverse.churchlitebackend.config.registryservice;

import com.potatotech.authorization.stereotype.Anonymous;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Hashtable;

@FeignClient(name = "registryService", url = "http://localhost:5000")
public interface RegistryService {

    @Anonymous
    @PostMapping("/rest/registry")
    void registryService(Hashtable hash);
}
