/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.web.rest.service;

import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves tenant enumeration to the other apps' remote clients. Hosted by the app that owns the datasource
 * (configuration-app), so datasource-less apps — notably coordinator-app, whose per-tenant sweeps need the tenant list
 * — can resolve tenants without taking a database dependency.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/tenant-service")
public class RemoteTenantServiceController {

    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public RemoteTenantServiceController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-tenant-ids",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<String>> getTenantIds() {
        return ResponseEntity.ok(tenantService.getTenantIds());
    }
}
