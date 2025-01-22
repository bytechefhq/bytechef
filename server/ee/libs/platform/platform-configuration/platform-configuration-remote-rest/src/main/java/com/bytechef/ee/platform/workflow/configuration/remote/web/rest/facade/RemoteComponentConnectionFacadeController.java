/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.web.rest.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
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
@RequestMapping("/remote/component-connection-facade")
public class RemoteComponentConnectionFacadeController {

    private final ComponentConnectionFacade componentConnectionFacade;

    @SuppressFBWarnings("EI")
    public RemoteComponentConnectionFacadeController(ComponentConnectionFacade componentConnectionFacade) {
        this.componentConnectionFacade = componentConnectionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-workflow-task-connections",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentConnection>> getWorkflowConnections(@RequestBody WorkflowTask workflowTask) {
        return ResponseEntity.ok(componentConnectionFacade.getComponentConnections(workflowTask));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-workflow-trigger-connections",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ComponentConnection>> getWorkflowConnections(
        @RequestBody WorkflowTrigger workflowTrigger) {

        return ResponseEntity.ok(componentConnectionFacade.getComponentConnections(workflowTrigger));
    }
}
