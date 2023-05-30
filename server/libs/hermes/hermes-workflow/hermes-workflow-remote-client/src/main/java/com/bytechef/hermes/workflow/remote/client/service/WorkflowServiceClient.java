
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

package com.bytechef.hermes.workflow.remote.client.service;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkflowServiceClient implements WorkflowService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public WorkflowServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public Workflow create(String definition, Workflow.Format format, Workflow.SourceType sourceType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(String id) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/workflow-service/get-workflow/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(Workflow.class)
            .block();
    }

    @Override
    public List<Workflow> getWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow update(String id, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Workflow> getWorkflows(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }
}
