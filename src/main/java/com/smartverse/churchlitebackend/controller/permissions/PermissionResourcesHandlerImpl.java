package com.smartverse.churchlitebackend.controller.permissions;

import com.smartverse.churchlitebackend.config.metadata.PermissionCatalogService;

import com.smartverse.churchlitebackend_gen.endpoints.GetPermissionResources;
import com.smartverse.churchlitebackend_gen.endpoints.GetPermissionResourcesOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class PermissionResourcesHandlerImpl implements GetPermissionResources {
    private final PermissionCatalogService catalog;

    public PermissionResourcesHandlerImpl(PermissionCatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public ResponseEntity<GetPermissionResourcesOutput> getPermissionResources() {
        var output = new GetPermissionResourcesOutput();
        output.resources = catalog.resources();
        return ResponseEntity.ok(output);
    }
}
