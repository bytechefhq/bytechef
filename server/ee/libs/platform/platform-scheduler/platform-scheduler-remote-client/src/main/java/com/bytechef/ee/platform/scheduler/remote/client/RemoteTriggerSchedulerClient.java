/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
        Instant webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/schedule-dynamic-webhook-trigger-refresh")
                .build(),
            new DynamicWebhookRefreshTaskRequest(
                workflowExecutionId, webhookExpirationDate, componentName, componentVersion, connectionId));
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

    @Override
    public void scheduleOneTimeTask(Instant executeAt, Map<String, ?> output, long jobId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(TRIGGER_SCHEDULER + "/schedule-one-time-task-resume")
                .build(),
            new ResumeOneTimeTaskRequest(executeAt, jobId, output));
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshTaskRequest(
        WorkflowExecutionId workflowExecutionId, Instant webhookExpirationDate, String componentName,
        int componentVersion, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    private record ResumeOneTimeTaskRequest(Instant executeAt, long jobId, Map<String, ?> continueParameters) {
    }

    @SuppressFBWarnings("EI")
    private record TriggerWorkflowTaskRequest(
        WorkflowExecutionId workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {
    }
}
