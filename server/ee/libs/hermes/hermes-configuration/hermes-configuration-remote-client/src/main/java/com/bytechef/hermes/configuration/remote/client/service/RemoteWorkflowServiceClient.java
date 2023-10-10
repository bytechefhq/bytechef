
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.configuration.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteWorkflowServiceClient implements WorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String WORKFLOW_SERVICE = "/remote/workflow-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteWorkflowServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Workflow create(
        @NonNull String definition, @NonNull Workflow.Format format, @NonNull Workflow.SourceType sourceType,
        int type) {
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
    public List<Workflow> getFilesystemWorkflows(int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(@NonNull String id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflow/{id}")
                .build(id),
            Workflow.class);
    }

    @Override
    public List<Workflow> getWorkflows(int type) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflows/{type}")
                .build(type),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Workflow> getWorkflows(@NonNull List<String> workflowIds) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/get-workflows/" + String.join(",", workflowIds))
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public void refreshCache(@NonNull String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow update(@NonNull String id, @NonNull String definition) {
        throw new UnsupportedOperationException();
    }
}
