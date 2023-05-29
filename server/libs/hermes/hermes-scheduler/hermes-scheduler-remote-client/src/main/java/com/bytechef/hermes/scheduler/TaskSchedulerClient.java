
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
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskSchedulerClient implements TaskScheduler {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    public TaskSchedulerClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public void cancelRefreshDynamicWebhookTriggerTask(String workflowExecutionId) {
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
    public void cancelPollTriggerTask(String workflowExecutionId) {
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
    public void cancelTriggerWorkflowTask(String workflowExecutionId) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/cancel-schedule-task")
                .build())
            .bodyValue(workflowExecutionId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void scheduleRefreshDynamicWebhookTriggerTask(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {

        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/schedule-poll-task")
                .build())
            .bodyValue(new DynamicWebhookRefreshTaskRequest(
                workflowExecutionId, webhookExpirationDate, componentName, componentVersion))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    public void schedulePollTriggerTask(WorkflowExecutionId workflowExecutionId) {
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

    @Override
    public void scheduleTriggerWorkflowTask(
        String workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {

        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("scheduler-service-app")
                .path("/trigger-scheduler/schedule-poll-task")
                .build())
            .bodyValue(new ScheduleTaskRequest(workflowExecutionId, pattern, zoneId, output))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshTaskRequest(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {
    }

    @SuppressFBWarnings("EI")
    private record ScheduleTaskRequest(
        String workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {
    }
}
