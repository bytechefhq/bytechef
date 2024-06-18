/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.web.rest.service;

import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
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
@RequestMapping("/remote/workflow-test-configuration-service")
public class RemoteWorkflowTestConfigurationServiceController {

    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    public RemoteWorkflowTestConfigurationServiceController(
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/is-connection-used/{connectionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Boolean> isConnectionUsed(@PathVariable long connectionId) {
        return ResponseEntity.ok(workflowTestConfigurationService.isConnectionUsed(connectionId));
    }
}
