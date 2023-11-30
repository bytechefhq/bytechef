/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.configuration.remote.web.rest.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/remote/workflow-connection-facade")
public class RemoteWorkflowConnectionFacadeController {

    private final WorkflowConnectionFacade workflowConnectionFacade;

    @SuppressFBWarnings("EI")
    public RemoteWorkflowConnectionFacadeController(WorkflowConnectionFacade workflowConnectionFacade) {
        this.workflowConnectionFacade = workflowConnectionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-workflow-task-connections",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<WorkflowConnection>> getWorkflowConnections(@RequestBody WorkflowTask workflowTask) {
        return ResponseEntity.ok(workflowConnectionFacade.getWorkflowConnections(workflowTask));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-workflow-trigger-connections",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<WorkflowConnection>> getWorkflowConnections(
        @RequestBody WorkflowTrigger workflowTrigger) {

        return ResponseEntity.ok(workflowConnectionFacade.getWorkflowConnections(workflowTrigger));
    }
}
