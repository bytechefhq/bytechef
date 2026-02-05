/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
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
public class RemoteComponentConnectionFacadeClient implements ComponentConnectionFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String WORKFLOW_CONNECTION_FACADE = "/remote/component-connection-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteComponentConnectionFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<ComponentConnection> getClusterElementComponentConnections(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentConnection getComponentConnection(String workflowId, String workflowNodeName, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTask workflowTask) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_CONNECTION_FACADE + "/get-workflow-task-connections")
                .build(),
            workflowTask,
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTrigger workflowTrigger) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_CONNECTION_FACADE + "/get-workflow-trigger-connections")
                .build(),
            workflowTrigger,
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<ComponentConnection> getWorkflowNodeComponentConnections(String workflowId, String workflowNodeName) {
        throw new UnsupportedOperationException();
    }
}
