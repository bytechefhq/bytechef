/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.configuration.remote.web.rest.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
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
@RequestMapping("/remote/workflow-service")
public class RemoteWorkflowServiceController {

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public RemoteWorkflowServiceController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflow/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Workflow> getWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflows/{type}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Workflow>> getWorkflows(@PathVariable int type) {
        return ResponseEntity.ok(workflowService.getWorkflows(type));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflows-by-ids/{workflowIds}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Workflow>> getWorkflowsByIds(@PathVariable String workflowIds) {
        Stream<String> stream = Arrays.stream(workflowIds.split(","));

        return ResponseEntity.ok(workflowService.getWorkflows(stream.toList()));
    }
}
