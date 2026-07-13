package com.smartverse.churchlitebackend.config.registryservice;


import com.smartverse.churchlitebackend.config.context.EnumConfigContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Hashtable;

@Service
public class RegistryServiceImpl{

    private final RegistryService registryService;

    @Autowired
    public RegistryServiceImpl(RegistryService feignClient) {
        this.registryService = feignClient;
    }
}
