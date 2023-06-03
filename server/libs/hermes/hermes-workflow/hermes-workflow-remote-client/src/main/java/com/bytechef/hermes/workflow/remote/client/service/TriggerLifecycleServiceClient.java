
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

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.workflow.service.TriggerLifecycleService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerLifecycleServiceClient implements TriggerLifecycleService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public TriggerLifecycleServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public <T> Optional<T> fetchValue(String workflowExecutionId) {
        return Optional.ofNullable(
            loadBalancedWebClientBuilder
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                    .host("platform-service-app")
                    .path("/api/internal/trigger-lifecycle-service/fetch-value/{workflowExecutionId}")
                    .build(workflowExecutionId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<T>() {})
                .block());
    }

    @Override
    public void save(String workflowExecutionId, DynamicWebhookEnableOutput value) {
        loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/trigger-lifecycle-service/save/{workflowExecutionId}")
                .build(workflowExecutionId))
            .bodyValue(value)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
