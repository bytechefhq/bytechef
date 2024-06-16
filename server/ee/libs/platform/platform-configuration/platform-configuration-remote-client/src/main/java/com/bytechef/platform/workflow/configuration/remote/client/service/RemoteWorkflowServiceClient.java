/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.configuration.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteWorkflowServiceClient implements WorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String WORKFLOW_SERVICE = "/remote/workflow-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteWorkflowServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public Workflow create(
        @NonNull String definition, @NonNull Workflow.Format format, @NonNull Workflow.SourceType sourceType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(@NonNull String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow duplicateWorkflow(@NonNull String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Workflow> getWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(@NonNull String id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflow/{id}")
                .build(id),
            Workflow.class);
    }

    @Override
    public List<Workflow> getWorkflows(@NonNull List<String> workflowIds) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflows-by-ids/" + String.join(",", workflowIds))
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public void refreshCache(@NonNull String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow update(@NonNull String id, @NonNull String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
