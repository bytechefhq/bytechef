
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

package com.bytechef.hermes.execution.remote.client.facade;

import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerLifecycleFacadeClient implements TriggerLifecycleFacade {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public TriggerLifecycleFacadeClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public void executeTriggerDisable(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> triggerParameters, long connectionId) {

        post(
            "/api/internal/trigger-lifecycle-facade/execute-trigger-enable",
            new TriggerRequest(workflowExecutionId, triggerParameters, connectionId));
    }

    @Override
    public void executeTriggerEnable(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> triggerParameters, long connectionId) {

        post(
            "/api/internal/trigger-lifecycle-facade/execute-trigger-disable",
            new TriggerRequest(workflowExecutionId, triggerParameters, connectionId));
    }

    private void post(String path, TriggerRequest workflowExecutionId) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path(path)
                .build())
            .bodyValue(workflowExecutionId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }

    @SuppressFBWarnings("EI")
    private record TriggerRequest(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> triggerParameters, long connectionId) {
    }
}
