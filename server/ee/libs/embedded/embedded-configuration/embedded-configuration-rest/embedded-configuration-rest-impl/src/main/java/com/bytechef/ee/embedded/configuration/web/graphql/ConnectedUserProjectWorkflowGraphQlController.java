/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectedUserProjectWorkflowGraphQlController {

    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowGraphQlController(
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService) {
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
    }

    @QueryMapping
    public ConnectedUserProjectWorkflow connectedUserProjectWorkflow(@Argument long id) {
        return connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(id);
    }

    @QueryMapping
    public List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflowsByConnectedUserProjectId(
        @Argument long connectedUserProjectId) {

        return connectedUserProjectWorkflowService.getConnectedUserProjectWorkflows(connectedUserProjectId);
    }

    @QueryMapping
    public boolean isConnectionUsed(@Argument long connectionId) {
        return connectedUserProjectWorkflowService.isConnectionUsed(connectionId);
    }
}
