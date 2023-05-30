
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

import com.bytechef.hermes.workflow.domain.TriggerExecution;
import com.bytechef.hermes.workflow.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionServiceClient implements TriggerExecutionService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public TriggerExecutionServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/trigger-execution-service/create")
                .build())
            .bodyValue(triggerExecution)
            .retrieve()
            .bodyToMono(TriggerExecution.class)
            .block();
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/trigger-execution-service/get-trigger-execution/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(TriggerExecution.class)
            .block();
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/trigger-execution-service/update")
                .build())
            .bodyValue(triggerExecution)
            .retrieve()
            .bodyToMono(TriggerExecution.class)
            .block();
    }
}
