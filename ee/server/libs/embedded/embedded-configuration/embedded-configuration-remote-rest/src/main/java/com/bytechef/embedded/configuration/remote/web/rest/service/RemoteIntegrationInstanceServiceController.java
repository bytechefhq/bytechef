/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.web.rest.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/integration-instance-service")
public class RemoteIntegrationInstanceServiceController {

    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceServiceController(IntegrationInstanceService integrationInstanceService) {
        this.integrationInstanceService = integrationInstanceService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<IntegrationInstance> getIntegration(@PathVariable long id) {
        return ResponseEntity.ok(integrationInstanceService.getIntegrationInstance(id));
    }
}
