/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TriggerSubscriptionDeliveryTaskHandlerTest {

    private static final long PROJECT_DEPLOYMENT_ID = 7L;

    private PrincipalJobService principalJobService;
    private TriggerSubscriptionDeliveryTaskHandler triggerSubscriptionDeliveryTaskHandler;
    private TriggerSubscriptionEventSender triggerSubscriptionEventSender;
    private TriggerSubscriptionService triggerSubscriptionService;

    @BeforeEach
    void beforeEach() {
        principalJobService = mock(PrincipalJobService.class);
        triggerSubscriptionEventSender = mock(TriggerSubscriptionEventSender.class);
        triggerSubscriptionService = mock(TriggerSubscriptionService.class);

        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID));

        triggerSubscriptionDeliveryTaskHandler = new TriggerSubscriptionDeliveryTaskHandler(
            principalJobService, triggerSubscriptionEventSender, triggerSubscriptionService);
    }

    @Test
    void testHandleSendsOutputWithJobId() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.ENABLED);

        when(triggerSubscriptionService.fetchTriggerSubscription(11L)).thenReturn(Optional.of(triggerSubscription));

        Map<String, Object> output = Map.of("id", "c1");

        Void result = triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(output));

        assertThat(result).isNull();

        verify(triggerSubscriptionEventSender).sendTriggerEvent(triggerSubscription, output, 42L);
    }

    @Test
    void testHandleSkipsJobOfAnotherDeployment() {
        when(triggerSubscriptionService.fetchTriggerSubscription(11L))
            .thenReturn(Optional.of(createTriggerSubscription(TriggerSubscriptionStatus.ENABLED)));
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(99L));

        triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleSkipsJobWithoutPrincipal() {
        when(triggerSubscriptionService.fetchTriggerSubscription(11L))
            .thenReturn(Optional.of(createTriggerSubscription(TriggerSubscriptionStatus.ENABLED)));
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());

        triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleSkipsDisabledSubscription() {
        when(triggerSubscriptionService.fetchTriggerSubscription(11L))
            .thenReturn(Optional.of(createTriggerSubscription(TriggerSubscriptionStatus.DISABLED)));

        triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @ParameterizedTest
    @EnumSource(value = TriggerSubscriptionStatus.class, names = {
        "DISABLED", "FAILED"
    })
    void testHandleHoldsEventWhileEnableIsInFlightAndDeliversItOnceEnabled(TriggerSubscriptionStatus status) {
        TriggerSubscription enablingTriggerSubscription = createTriggerSubscription(status);

        enablingTriggerSubscription.setRegistrationLeaseUntil(Instant.now()
            .plus(Duration.ofMinutes(5)));

        TriggerSubscription enabledTriggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.ENABLED);

        when(triggerSubscriptionService.fetchTriggerSubscription(11L))
            .thenReturn(Optional.of(enablingTriggerSubscription), Optional.of(enabledTriggerSubscription));

        Map<String, Object> output = Map.of("id", "c1");

        TaskExecution taskExecution = createTaskExecution(output);

        assertThatThrownBy(() -> triggerSubscriptionDeliveryTaskHandler.handle(taskExecution))
            .isInstanceOf(TriggerSubscriptionEnablingException.class);

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());

        triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(output));

        verify(triggerSubscriptionEventSender).sendTriggerEvent(enabledTriggerSubscription, output, 42L);
    }

    @Test
    void testHandleDropsHeldEventWhenEnableEndsFailed() {
        TriggerSubscription enablingTriggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.FAILED);

        enablingTriggerSubscription.setRegistrationLeaseUntil(Instant.now()
            .plus(Duration.ofMinutes(5)));

        when(triggerSubscriptionService.fetchTriggerSubscription(11L))
            .thenReturn(
                Optional.of(enablingTriggerSubscription),
                Optional.of(createTriggerSubscription(TriggerSubscriptionStatus.FAILED)));

        TaskExecution taskExecution = createTaskExecution(Map.of("id", "c1"));

        assertThatThrownBy(() -> triggerSubscriptionDeliveryTaskHandler.handle(taskExecution))
            .isInstanceOf(TriggerSubscriptionEnablingException.class);

        Void result = triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        assertThat(result).isNull();

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleDropsEventOfDisabledSubscriptionWithExpiredLease() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.DISABLED);

        triggerSubscription.setRegistrationLeaseUntil(Instant.now()
            .minus(Duration.ofMinutes(1)));

        when(triggerSubscriptionService.fetchTriggerSubscription(11L)).thenReturn(Optional.of(triggerSubscription));

        Void result = triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        assertThat(result).isNull();

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleDropsEventOfAnotherDeploymentWhileEnableIsInFlight() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.FAILED);

        triggerSubscription.setRegistrationLeaseUntil(Instant.now()
            .plus(Duration.ofMinutes(5)));

        when(triggerSubscriptionService.fetchTriggerSubscription(11L)).thenReturn(Optional.of(triggerSubscription));
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.of(99L));

        Void result = triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        assertThat(result).isNull();

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleSkipsMissingSubscription() {
        when(triggerSubscriptionService.fetchTriggerSubscription(11L)).thenReturn(Optional.empty());

        triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        verify(triggerSubscriptionEventSender, never()).sendTriggerEvent(any(), any(), anyLong());
    }

    @Test
    void testHandleReturnsNormallyWhenSenderThrows() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(TriggerSubscriptionStatus.ENABLED);

        when(triggerSubscriptionService.fetchTriggerSubscription(11L)).thenReturn(Optional.of(triggerSubscription));

        doThrow(new IllegalStateException("boom")).when(triggerSubscriptionEventSender)
            .sendTriggerEvent(any(), any(), anyLong());

        Void result = triggerSubscriptionDeliveryTaskHandler.handle(createTaskExecution(Map.of("id", "c1")));

        assertThat(result).isNull();
    }

    private static TaskExecution createTaskExecution(Object output) {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "name", "deliver_1", "type", "triggerSubscription/v1/deliver",
                "parameters", Map.of("subscriptionId", 11, "output", output)));

        return TaskExecution.builder()
            .jobId(42L)
            .workflowTask(workflowTask)
            .build();
    }

    private static TriggerSubscription createTriggerSubscription(TriggerSubscriptionStatus status) {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        triggerSubscription.setStatus(status);

        return triggerSubscription;
    }
}
