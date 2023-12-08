/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.scheduler.remote.client;

import com.bytechef.commons.restclient.LoadBalancedRestClient;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerSchedulerClient implements TriggerScheduler {

    private static final String SCHEDULER_APP = "scheduler-app";
    private static final String TRIGGER_SCHEDULER = "/remote/trigger-scheduler";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerSchedulerClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/cancel-dynamic-webhook-trigger-refresh")
                .build(),
            workflowExecutionId);
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/cancel-polling-trigger")
                .build(),
            workflowExecutionId);
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/cancel-schedule-trigger")
                .build(),
            workflowExecutionId);
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId) {

        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/schedule-dynamic-webhook-trigger-refresh")
                .build(),
            new DynamicWebhookRefreshTaskRequest(
                workflowExecutionId, webhookExpirationDate, componentName, componentVersion));
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/schedule-polling-trigger")
                .build(),
            workflowExecutionId);
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/schedule-schedule-trigger")
                .build(),
            new TriggerWorkflowTaskRequest(workflowExecutionId, pattern, zoneId, output));
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshTaskRequest(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {
    }

    @SuppressFBWarnings("EI")
    private record TriggerWorkflowTaskRequest(
        WorkflowExecutionId workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {
    }
}
