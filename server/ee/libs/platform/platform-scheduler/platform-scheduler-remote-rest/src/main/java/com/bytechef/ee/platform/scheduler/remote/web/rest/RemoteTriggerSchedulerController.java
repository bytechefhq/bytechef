/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.web.rest;

import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/trigger-scheduler")
public class RemoteTriggerSchedulerController {

    private final TriggerScheduler triggerScheduler;

    public RemoteTriggerSchedulerController(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/cancel-dynamic-webhook-trigger-refresh",
        consumes = {
            "application/json"
        })
    void cancelDynamicWebhookRefreshTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-polling-trigger",
        consumes = {
            "application/json"
        })
    void cancelPollTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelPollingTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-schedule-trigger",
        consumes = {
            "application/json"
        })
    void cancelTriggerWorkflowTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-dynamic-webhook-trigger-refresh",
        consumes = {
            "application/json"
        })
    void scheduleDynamicWebhookRefreshTask(
        @Valid @RequestBody DynamicWebhookRefreshTaskRequest dynamicWebhookRefreshTaskRequest) {

        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            dynamicWebhookRefreshTaskRequest.webhookExpirationDate, dynamicWebhookRefreshTaskRequest.componentName,
            dynamicWebhookRefreshTaskRequest.componentVersion, dynamicWebhookRefreshTaskRequest.workflowExecutionId,
            dynamicWebhookRefreshTaskRequest.connectionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-polling-trigger",
        consumes = {
            "application/json"
        })
    void schedulePollTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        triggerScheduler.schedulePollingTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-schedule-trigger",
        consumes = {
            "application/json"
        })
    void scheduleTriggerWorkflowTask(@Valid @RequestBody TriggerWorkflowTaskRequest triggerWorkflowTaskRequest) {
        triggerScheduler.scheduleScheduleTrigger(
            triggerWorkflowTaskRequest.pattern, triggerWorkflowTaskRequest.zoneId, triggerWorkflowTaskRequest.output,
            WorkflowExecutionId.parse(triggerWorkflowTaskRequest.workflowExecutionId));
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshTaskRequest(
        Instant webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    private record TriggerWorkflowTaskRequest(
        String pattern, String zoneId, Map<String, Object> output, String workflowExecutionId) {
    }
}
