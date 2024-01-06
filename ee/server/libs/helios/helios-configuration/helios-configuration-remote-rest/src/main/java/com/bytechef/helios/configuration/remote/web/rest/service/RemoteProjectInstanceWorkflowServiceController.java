/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.helios.configuration.remote.web.rest.service;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
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
@RequestMapping("/remote/project-instance-workflow-service")
public class RemoteProjectInstanceWorkflowServiceController {

    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceWorkflowServiceController(
        ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
            "{workflowConnectionOperationName}/{workflowConnectionKey}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstanceWorkflowConnection> fetchProjectInstanceWorkflowConnection(
        @PathVariable long projectInstanceId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName, @PathVariable String workflowConnectionKey) {

        return ResponseEntity.ok(
            projectInstanceWorkflowService.fetchProjectInstanceWorkflowConnection(
                projectInstanceId, workflowId, workflowConnectionOperationName, workflowConnectionKey)
                .orElse(null));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-instance-workflow/{projectInstanceId}/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstanceWorkflow> getProjectInstanceWorkflow(
        @PathVariable long projectInstanceId, @PathVariable String workflowId) {

        return ResponseEntity.ok(
            projectInstanceWorkflowService.getProjectInstanceWorkflow(projectInstanceId, workflowId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
            "{workflowConnectionOperationName}/{workflowConnectionKey}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstanceWorkflowConnection> getProjectInstanceWorkflowConnection(
        @PathVariable long projectInstanceId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName, @PathVariable String workflowConnectionKey) {

        return ResponseEntity.ok(
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(
                projectInstanceId, workflowId, workflowConnectionOperationName, workflowConnectionKey));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
            "{workflowConnectionOperationName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ProjectInstanceWorkflowConnection>> getProjectInstanceWorkflowConnection(
        @PathVariable long projectInstanceId, @PathVariable String workflowId,
        @PathVariable String workflowConnectionOperationName) {

        return ResponseEntity.ok(
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnections(
                projectInstanceId, workflowId, workflowConnectionOperationName));
    }
}
