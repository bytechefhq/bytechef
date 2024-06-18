/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
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
@RequestMapping("/remote/integration-instance-configuration-service")
public class RemoteIntegrationInstanceConfigurationServiceController {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceConfigurationServiceController(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance-configuration/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<IntegrationInstanceConfiguration> getIntegrationInstanceConfiguration(@PathVariable long id) {
        return ResponseEntity.ok(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(id));
    }
}
