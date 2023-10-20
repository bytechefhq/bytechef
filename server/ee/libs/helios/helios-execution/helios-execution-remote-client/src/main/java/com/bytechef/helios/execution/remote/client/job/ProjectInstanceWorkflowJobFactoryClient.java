
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

package com.bytechef.helios.execution.remote.client.job;

import com.bytechef.helios.execution.job.factory.ProjectInstanceWorkflowJobFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceWorkflowJobFactoryClient implements ProjectInstanceWorkflowJobFactory {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public ProjectInstanceWorkflowJobFactoryClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    @SuppressFBWarnings("NP")
    public long createJob(long instanceId, String workflowId) {
        return loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/project-instance-workflow-job-factory/create-job/{instanceId}/{workflowId}")
                .build(instanceId, workflowId))
            .retrieve()
            .bodyToMono(Long.class)
            .block();
    }
}
