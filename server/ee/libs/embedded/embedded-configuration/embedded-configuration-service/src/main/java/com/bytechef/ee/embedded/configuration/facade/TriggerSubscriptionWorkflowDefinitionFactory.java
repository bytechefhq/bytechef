/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.JsonUtils;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the workflow a subscription deploys: its trigger and a deliver task that hands the trigger output to the
 * subscription's webhook. The deliver task retries while the subscription is being enabled, because a trigger may fire
 * after it is registered but before the ENABLED status commits; its retries cover the registration lease, the longest a
 * request can hold an enable without renewing its lease.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class TriggerSubscriptionWorkflowDefinitionFactory {

    public static final String DELIVER_TASK_NAME = "deliver_1";
    public static final String DELIVER_TASK_TYPE = "triggerSubscription/v1/deliver";
    public static final String TRIGGER_NAME = "trigger_1";

    private TriggerSubscriptionWorkflowDefinitionFactory() {
    }

    public static String create(
        String componentName, int componentVersion, String triggerName, Map<String, ?> parameters,
        long subscriptionId, Duration registrationLease) {

        Map<String, Object> triggerMap = new LinkedHashMap<>();

        triggerMap.put("name", TRIGGER_NAME);
        triggerMap.put("type", "%s/v%d/%s".formatted(componentName, componentVersion, triggerName));
        triggerMap.put("parameters", new LinkedHashMap<>(parameters));

        Map<String, Object> deliverParameters = new LinkedHashMap<>();

        deliverParameters.put("subscriptionId", subscriptionId);
        deliverParameters.put("output", "${" + TRIGGER_NAME + "}");

        Map<String, Object> deliverTaskMap = new LinkedHashMap<>();

        deliverTaskMap.put("name", DELIVER_TASK_NAME);
        deliverTaskMap.put("type", DELIVER_TASK_TYPE);
        deliverTaskMap.put("parameters", deliverParameters);
        deliverTaskMap.put(WorkflowConstants.MAX_RETRIES, getDeliverMaxRetries(registrationLease));

        Map<String, Object> workflowMap = new LinkedHashMap<>();

        workflowMap.put("label", "%s/%s subscription %d".formatted(componentName, triggerName, subscriptionId));
        workflowMap.put("triggers", List.of(triggerMap));
        workflowMap.put("tasks", List.of(deliverTaskMap));

        return JsonUtils.write(workflowMap);
    }

    /**
     * Returns the fewest retries of the deliver task whose delays add up to at least the registration lease. The
     * coordinator delays a retry by the delay of a task execution with that retry attempt, so the delays are summed
     * from the same calculation.
     */
    public static int getDeliverMaxRetries(Duration registrationLease) {
        long registrationLeaseMillis = registrationLease.toMillis();
        long retryDelayMillis = 0;
        int retryAttempts = 0;

        do {
            retryAttempts++;

            TaskExecution taskExecution = TaskExecution.builder()
                .retryAttempts(retryAttempts)
                .build();

            long attemptRetryDelayMillis = taskExecution.getRetryDelayMillis();

            if (attemptRetryDelayMillis <= 0) {
                throw new IllegalStateException("Task retry delay must be greater than 0");
            }

            retryDelayMillis += attemptRetryDelayMillis;
        } while (retryDelayMillis < registrationLeaseMillis);

        return retryAttempts;
    }
}
