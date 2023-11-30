/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.configuration.remote.client.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowConnectionFacadeImpl implements WorkflowConnectionFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String WORKFLOW_CONNECTION_FACADE = "/remote/workflow-connection-facade";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public WorkflowConnectionFacadeImpl(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTask workflowTask) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_CONNECTION_FACADE + "/get-workflow-task-connections")
                .build(),
            workflowTask,
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTrigger workflowTrigger) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_CONNECTION_FACADE + "/get-workflow-trigger-connections")
                .build(),
            workflowTrigger,
            new ParameterizedTypeReference<>() {});
    }
}
