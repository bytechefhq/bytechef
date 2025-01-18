/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
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
@RequestMapping("/remote/project/deployment-workflow-service")
public class RemoteProjectDeploymentWorkflowServiceController {

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public RemoteProjectDeploymentWorkflowServiceController(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/is-connection-used/{connectionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Boolean> isConnectionUsed(@PathVariable long connectionId) {
        return ResponseEntity.ok(projectDeploymentWorkflowService.isConnectionUsed(connectionId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project/deployment-workflow/{projectDeploymentId}/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectDeploymentWorkflow> getProjectDeploymentWorkflow(
        @PathVariable long projectDeploymentId, @PathVariable String workflowId) {

        return ResponseEntity.ok(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentId, workflowId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project/deployment-workflow-connection/{projectDeploymentId}/{workflowId}/" +
            "{workflowNodeName}/{workflowConnectionKey}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectDeploymentWorkflowConnection> getProjectDeploymentWorkflowConnection(
        @PathVariable long projectDeploymentId, @PathVariable String workflowId,
        @PathVariable String workflowNodeName, @PathVariable String workflowConnectionKey) {

        return ResponseEntity.ok(
            projectDeploymentWorkflowService
                .fetchProjectDeploymentWorkflowConnection(
                    projectDeploymentId, workflowId, workflowNodeName, workflowConnectionKey)
                .orElse(null));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project/deployment-workflow-connection/{projectDeploymentId}/{workflowId}/" +
            "{workflowConnectionOperationName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ProjectDeploymentWorkflowConnection>> getProjectDeploymentWorkflowConnection(
        @PathVariable long projectDeploymentId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName) {

        return ResponseEntity.ok(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
                projectDeploymentId, workflowId, workflowConnectionOperationName));
    }
}
