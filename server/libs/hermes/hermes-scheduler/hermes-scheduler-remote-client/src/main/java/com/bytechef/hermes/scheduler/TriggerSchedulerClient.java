
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

package com.bytechef.hermes.scheduler;

import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerClient implements TriggerScheduler {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    public TriggerSchedulerClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public void cancelDynamicWebhookRefreshTask(WorkflowExecutionId workflowExecutionId) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/cancel-dynamic-webhook-refresh-task")
                .build())
            .bodyValue(workflowExecutionId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void cancelPollTask(WorkflowExecutionId workflowExecutionId) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/cancel-poll-task")
                .build())
            .bodyValue(workflowExecutionId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void scheduleDynamicWebhookRefreshTask(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {

        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/schedule-poll-task")
                .build())
            .bodyValue(new DynamicWebhookRefreshOneTimeTaskRequest(
                workflowExecutionId, webhookExpirationDate, componentName, componentVersion))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void schedulePollTask(WorkflowExecutionId workflowExecutionId) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/schedule-poll-task")
                .build())
            .bodyValue(workflowExecutionId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshOneTimeTaskRequest(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {
    }
}
