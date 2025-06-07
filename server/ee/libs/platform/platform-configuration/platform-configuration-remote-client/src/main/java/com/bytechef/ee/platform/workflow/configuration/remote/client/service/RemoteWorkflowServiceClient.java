/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
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
        String definition, Workflow.Format format, Workflow.SourceType sourceType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow duplicateWorkflow(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Workflow> fetchWorkflow(String id) {
        return Optional.empty();
    }

    @Override
    public List<Workflow> getWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(String id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflow/{id}")
                .build(id),
            Workflow.class);
    }

    @Override
    public List<Workflow> getWorkflows(List<String> workflowIds) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflows-by-ids/" + String.join(",", workflowIds))
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public void refreshCache(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow update(String id, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
