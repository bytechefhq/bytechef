/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.web.rest.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
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
@RequestMapping("/remote/integration-instance-workflow-service")
public class RemoteIntegrationInstanceWorkflowServiceController {

    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceWorkflowServiceController(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService) {

        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/is-connection-used/{connectionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Boolean> isConnectionUsed(@PathVariable long connectionId) {
        return ResponseEntity.ok(integrationInstanceWorkflowService.isConnectionUsed(connectionId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance-workflow/{integrationInstanceId}/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflow(
        @PathVariable long integrationInstanceId, @PathVariable String workflowId) {

        return ResponseEntity.ok(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(integrationInstanceId, workflowId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance-workflow-connection/{integrationInstanceId}/{workflowId}" +
            "/{workflowConnectionOperationName}/{workflowConnectionKey}",
        produces = {
            "application/json"
        })
    public ResponseEntity<IntegrationInstanceWorkflowConnection> getIntegrationInstanceWorkflowConnection(
        @PathVariable long integrationInstanceId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName, @PathVariable String workflowConnectionKey) {

        return ResponseEntity.ok(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflowConnection(
                integrationInstanceId, workflowId, workflowConnectionOperationName, workflowConnectionKey));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-integration-instance-workflow-connection/{integrationInstanceId}/{workflowId}" +
            "/{workflowConnectionOperationName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<IntegrationInstanceWorkflowConnection>> getIntegrationInstanceWorkflowConnection(
        @PathVariable long integrationInstanceId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName) {

        return ResponseEntity.ok(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflowConnections(
                integrationInstanceId, workflowId, workflowConnectionOperationName));
    }
}
