/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.web.rest.service;

import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.service.IntegrationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
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
@RequestMapping("/remote/integration-service")
public class RemoteIntegrationServiceController {

    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationServiceController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Integration> getIntegration(@PathVariable long id) {
        return ResponseEntity.ok(integrationService.getIntegration(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance-integration/{integrationInstanceId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Integration> getIntegrationInstanceIntegration(@PathVariable long integrationInstanceId) {
        return ResponseEntity.ok(integrationService.getIntegrationInstanceIntegration(integrationInstanceId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflow-integration/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Integration> getWorkflowIntegration(@PathVariable String workflowId) {
        return ResponseEntity.ok(integrationService.getWorkflowIntegration(workflowId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integrations",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Integration>> getIntegrations() {
        return ResponseEntity.ok(integrationService.getIntegrations());
    }
}
