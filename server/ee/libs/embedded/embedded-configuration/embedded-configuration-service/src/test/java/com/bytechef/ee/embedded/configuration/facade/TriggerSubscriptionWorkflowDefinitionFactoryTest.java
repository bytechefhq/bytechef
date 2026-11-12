/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TriggerSubscriptionWorkflowDefinitionFactoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void testCreateBuildsTriggerAndDeliverTask() {
        Map<String, Object> parameters = Map.of(
            "channel", "C123", "filter", Map.of("from", "${trigger_1.user}"), "limit", 5);

        String definition = TriggerSubscriptionWorkflowDefinitionFactory.create(
            "slack", 2, "newMessage", parameters, 42L, Duration.ofMinutes(5));

        Map<String, ?> workflowMap = JsonUtils.readMap(definition);

        assertThat(workflowMap.get("label")).isInstanceOf(String.class);
        assertThat((String) workflowMap.get("label")).isNotBlank();

        List<Map<String, ?>> triggers = (List<Map<String, ?>>) workflowMap.get("triggers");

        assertThat(triggers).hasSize(1);
        assertThat(triggers.getFirst()).containsOnlyKeys("name", "type", "parameters");
        assertThat(triggers.getFirst()
            .get("name")).isEqualTo("trigger_1");
        assertThat(triggers.getFirst()
            .get("type")).isEqualTo("slack/v2/newMessage");
        assertThat(triggers.getFirst()
            .get("parameters")).isEqualTo(parameters);

        List<Map<String, ?>> tasks = (List<Map<String, ?>>) workflowMap.get("tasks");

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst()).containsOnlyKeys("name", "type", "parameters", "maxRetries");
        assertThat(tasks.getFirst()
            .get("name")).isEqualTo("deliver_1");
        assertThat(tasks.getFirst()
            .get("type")).isEqualTo("triggerSubscription/v1/deliver");

        Map<String, ?> deliverParameters = (Map<String, ?>) tasks.getFirst()
            .get("parameters");

        assertThat(deliverParameters).containsOnlyKeys("subscriptionId", "output");
        assertThat(deliverParameters.get("subscriptionId")).isInstanceOf(Number.class);
        assertThat(((Number) deliverParameters.get("subscriptionId")).longValue()).isEqualTo(42L);
        assertThat(deliverParameters.get("output")).isEqualTo("${trigger_1}");

        assertThat(TriggerSubscriptionWorkflowDefinitionFactory.TRIGGER_NAME).isEqualTo("trigger_1");
        assertThat(TriggerSubscriptionWorkflowDefinitionFactory.DELIVER_TASK_TYPE)
            .isEqualTo("triggerSubscription/v1/deliver");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateRetriesTheDeliverTaskForTheRegistrationLease() {
        Duration registrationLease = Duration.ofMinutes(5);

        String definition = TriggerSubscriptionWorkflowDefinitionFactory.create(
            "slack", 2, "newMessage", Map.of(), 42L, registrationLease);

        Map<String, ?> workflowMap = JsonUtils.readMap(definition);

        List<Map<String, ?>> tasks = (List<Map<String, ?>>) workflowMap.get("tasks");

        WorkflowTask workflowTask = new WorkflowTask(tasks.getFirst());

        int maxRetries = workflowTask.getMaxRetries();

        assertThat(maxRetries).isEqualTo(17);
        assertThat(getRetryDelayMillis(maxRetries)).isGreaterThanOrEqualTo(registrationLease.toMillis());
        assertThat(getRetryDelayMillis(maxRetries - 1)).isLessThan(registrationLease.toMillis());
    }

    @Test
    void testGetDeliverMaxRetriesRetriesAtLeastOnce() {
        assertThat(TriggerSubscriptionWorkflowDefinitionFactory.getDeliverMaxRetries(Duration.ofMillis(1)))
            .isEqualTo(1);
        assertThat(TriggerSubscriptionWorkflowDefinitionFactory.getDeliverMaxRetries(Duration.ofSeconds(2)))
            .isEqualTo(1);
        assertThat(TriggerSubscriptionWorkflowDefinitionFactory.getDeliverMaxRetries(Duration.ofSeconds(3)))
            .isEqualTo(2);
    }

    private static long getRetryDelayMillis(int retries) {
        long retryDelayMillis = 0;

        for (int retryAttempt = 1; retryAttempt <= retries; retryAttempt++) {
            TaskExecution taskExecution = TaskExecution.builder()
                .retryAttempts(retryAttempt)
                .build();

            retryDelayMillis += taskExecution.getRetryDelayMillis();
        }

        return retryDelayMillis;
    }
}
