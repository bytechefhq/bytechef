
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.remote.client.service;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceWorkflowServiceClient implements ProjectInstanceWorkflowService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public ProjectInstanceWorkflowServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(String key, String operationName) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressFBWarnings("NP")
    public long getProjectInstanceWorkflowConnectionId(String key, String operationName) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path(
                    "/api/internal/project-instance-workflow-service/get-project-instance-workflow-connection-id" +
                        "/{key}/{taskName}")
                .build(key, operationName))
            .retrieve()
            .bodyToMono(Long.class)
            .block();
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path(
                    "/api/internal/project-instance-workflow-service/update-enabled/{id}/{enabled}")
                .build(id, enabled))
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
