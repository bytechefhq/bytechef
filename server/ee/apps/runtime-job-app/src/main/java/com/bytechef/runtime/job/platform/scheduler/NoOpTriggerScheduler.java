/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.platform.scheduler;

import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class NoOpTriggerScheduler implements TriggerScheduler {

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
    }
}
