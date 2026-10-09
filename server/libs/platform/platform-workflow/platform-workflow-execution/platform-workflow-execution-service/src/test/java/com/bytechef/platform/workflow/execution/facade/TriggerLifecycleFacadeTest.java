/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
public class TriggerLifecycleFacadeTest {

    private static final WorkflowNodeType WEBHOOK_TRIGGER_TYPE = new WorkflowNodeType("webhook", 1, "newEvent");

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private TriggerScheduler triggerScheduler;

    @Mock
    private TriggerStateService triggerStateService;

    private TriggerLifecycleFacadeImpl triggerLifecycleFacade;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() {
        triggerLifecycleFacade = new TriggerLifecycleFacadeImpl(
            triggerScheduler, triggerDefinitionFacade, triggerDefinitionService, triggerStateService);
        workflowExecutionId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "workflow-uuid", "trigger_1");
    }

    @Test
    public void testExecuteTriggerDisableReportsADisabledTrigger() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        when(triggerStateService.fetchValue(workflowExecutionId))
            .thenReturn(Optional.of(new WebhookEnableOutput(Map.of("id", "subscription-1"), null)));

        boolean disabled = triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of("event", "trigger_1"), 42L);

        assertThat(disabled).isTrue();

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of("event", "trigger_1"), workflowExecutionId.toString(),
            Map.of("id", "subscription-1"), 42L);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verify(triggerStateService).delete(workflowExecutionId);
    }

    @Test
    public void testExecuteTriggerDisableReportsATriggerTheProviderCouldNotDisable() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("webhook deregistration refused"))
            .when(triggerDefinitionFacade)
            .executeWebhookDisable(anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());

        boolean disabled = triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of("event", "trigger_1"), null);

        assertThat(disabled).isFalse();

        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerDisableReportsADisabledTriggerWhoseStateCouldNotBeDeleted() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        when(triggerStateService.fetchValue(workflowExecutionId))
            .thenReturn(Optional.of(new WebhookEnableOutput(Map.of("id", "subscription-1"), null)));
        doThrow(new IllegalStateException("trigger state delete refused"))
            .when(triggerStateService)
            .delete(workflowExecutionId);

        boolean disabled = triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of("event", "trigger_1"), 42L);

        assertThat(disabled).isTrue();

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of("event", "trigger_1"), workflowExecutionId.toString(),
            Map.of("id", "subscription-1"), 42L);
    }

    @Test
    public void testExecuteTriggerDisableReportsADisabledTriggerWhoseRefreshCouldNotBeCancelled() {
        stubTriggerType(TriggerType.HYBRID);

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("refresh cancellation refused"))
            .when(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());

        boolean disabled = triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), null);

        assertThat(disabled).isTrue();

        verify(triggerStateService).delete(workflowExecutionId);
    }

    @Test
    public void testExecuteTriggerDisableReportsADisabledPollingTrigger() {
        stubTriggerType(TriggerType.POLLING);

        boolean disabled = triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), null);

        assertThat(disabled).isTrue();

        verify(triggerScheduler).cancelPollingTrigger(workflowExecutionId.toString());
        verify(triggerDefinitionFacade, never()).executeWebhookDisable(
            anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());
    }

    @Test
    public void testExecuteTriggerEnableUnsubscribesAWebhookWhoseStateCouldNotBeSaved() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(
            triggerDefinitionFacade.executeWebhookEnable(
                "webhook", 1, "newEvent", Map.of("event", "trigger_1"), workflowExecutionId.toString(), 42L,
                "https://hooks.example/1", 1L))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of("event", "trigger_1"), 42L,
            "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        assertThat(saveFailure.getSuppressed()).isEmpty();

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of("event", "trigger_1"), workflowExecutionId.toString(),
            Map.of("id", "subscription-1"), 42L);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
    }

    @Test
    public void testExecuteTriggerEnableUnsubscribesAWebhookWhoseRefreshCouldNotBeScheduled() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        Instant webhookExpirationDate = Instant.parse("2026-12-01T00:00:00Z");
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-1"), webhookExpirationDate);
        IllegalStateException scheduleFailure = new IllegalStateException("refresh scheduling refused");

        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(scheduleFailure).when(triggerScheduler)
            .scheduleDynamicWebhookTriggerRefresh(webhookExpirationDate, "webhook", 1, workflowExecutionId, null);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), null, "https://hooks.example/1", 1L))
                .isSameAs(scheduleFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of(), workflowExecutionId.toString(), Map.of("id", "subscription-1"), null);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verify(triggerStateService).delete(workflowExecutionId);
    }

    @Test
    public void testExecuteTriggerEnableSuppressesTheFailuresOfItsCompensation() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");
        IllegalStateException cancelFailure = new IllegalStateException("refresh cancellation refused");
        IllegalStateException deleteFailure = new IllegalStateException("trigger state delete refused");

        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);
        doThrow(cancelFailure).when(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        doThrow(deleteFailure).when(triggerStateService)
            .delete(workflowExecutionId);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        assertThat(saveFailure.getSuppressed()).containsExactly(cancelFailure, deleteFailure);
    }

    @Test
    public void testExecuteTriggerEnableRestoresTheEarlierWebhookStateWhenTheNewStateCouldNotBeSaved() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), null);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousWebhookEnableOutput));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        assertThat(saveFailure.getSuppressed()).isEmpty();

        InOrder inOrder = inOrder(triggerDefinitionFacade, triggerStateService);

        inOrder.verify(triggerDefinitionFacade)
            .executeWebhookDisable(
                "webhook", 1, "newEvent", Map.of(), workflowExecutionId.toString(), Map.of("id", "subscription-1"),
                42L);
        inOrder.verify(triggerStateService)
            .save(workflowExecutionId, previousWebhookEnableOutput);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerEnableReschedulesTheRefreshOfARestoredEarlierWebhookState() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        Instant previousWebhookExpirationDate = Instant.parse("2026-11-01T00:00:00Z");
        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), previousWebhookExpirationDate);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousWebhookEnableOutput));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        InOrder inOrder = inOrder(triggerScheduler, triggerStateService);

        inOrder.verify(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        inOrder.verify(triggerStateService)
            .save(workflowExecutionId, previousWebhookEnableOutput);
        inOrder.verify(triggerScheduler)
            .scheduleDynamicWebhookTriggerRefresh(previousWebhookExpirationDate, "webhook", 1, workflowExecutionId,
                42L);
    }

    @Test
    public void testExecuteTriggerEnableDeletesTheWebhookStateWhenThereWasNoEarlierState() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.empty());
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of(), workflowExecutionId.toString(), Map.of("id", "subscription-1"), 42L);
        verify(triggerStateService).delete(workflowExecutionId);
        verify(triggerStateService).save(any(), any());
    }

    @Test
    public void testExecuteTriggerEnableKeepsTheWebhookStateWhenTheNewSubscriptionCouldNotBeRemoved() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), null);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");
        IllegalStateException disableFailure = new IllegalStateException("webhook deregistration refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousWebhookEnableOutput));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);
        doThrow(disableFailure).when(triggerDefinitionFacade)
            .executeWebhookDisable(anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        assertThat(saveFailure.getSuppressed()).containsExactly(disableFailure);

        verify(triggerScheduler, never()).cancelDynamicWebhookTriggerRefresh(anyString());
        verify(triggerStateService, never()).delete(any());
        verify(triggerStateService, never()).save(workflowExecutionId, previousWebhookEnableOutput);
    }

    @Test
    public void testExecuteTriggerEnableRestoresAnEarlierWebhookStateReadAsAMap() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        Map<String, Object> previousTriggerState = Map.of(
            "parameters", Map.of("id", "subscription-armed-before-the-enable"),
            "webhookExpirationDate", "2026-12-01T00:00:00Z");
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousTriggerState));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        verify(triggerStateService).save(
            workflowExecutionId,
            new WebhookEnableOutput(
                Map.of("id", "subscription-armed-before-the-enable"), Instant.parse("2026-12-01T00:00:00Z")));
        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerEnableDeletesTheWebhookStateWhenTheEarlierStateCannotBeConverted() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        Map<String, Object> previousTriggerState = Map.of(
            "parameters", Map.of("id", "subscription-armed-before-the-enable"),
            "webhookExpirationDate", "not-a-date");
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousTriggerState));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);

        Logger logger = (Logger) LoggerFactory.getLogger(TriggerLifecycleFacadeImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
                "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1",
                1L))
                    .isSameAs(saveFailure);
        } finally {
            logger.detachAppender(listAppender);
        }

        verify(triggerStateService).delete(workflowExecutionId);
        verify(triggerStateService).save(any(), any());

        assertThat(listAppender.list)
            .anySatisfy(loggingEvent -> {
                assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
                assertThat(loggingEvent.getFormattedMessage()).contains("trigger_1", "workflow-1");
            });
    }

    @Test
    public void testExecuteTriggerEnableSuppressesAFailureToRestoreTheEarlierWebhookState() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), null);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);
        IllegalStateException saveFailure = new IllegalStateException("trigger state write refused");
        IllegalStateException restoreFailure = new IllegalStateException("trigger state restore refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.of(previousWebhookEnableOutput));
        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);
        doThrow(saveFailure).when(triggerStateService)
            .save(workflowExecutionId, webhookEnableOutput);
        doThrow(restoreFailure).when(triggerStateService)
            .save(workflowExecutionId, previousWebhookEnableOutput);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(saveFailure);

        assertThat(saveFailure.getSuppressed()).containsExactly(restoreFailure);
    }

    @Test
    public void testExecuteTriggerEnableEnablesNothingWhenTheEarlierWebhookStateCannotBeRead() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        IllegalStateException fetchFailure = new IllegalStateException("trigger state read refused");

        when(triggerStateService.fetchValue(workflowExecutionId)).thenThrow(fetchFailure);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(fetchFailure);

        verify(triggerDefinitionFacade, never()).executeWebhookEnable(
            anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong());
    }

    @Test
    public void testExecuteTriggerEnableMakesNoCompensationWhenTheProviderRefusesTheSubscription() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        IllegalStateException enableFailure = new IllegalStateException("webhook registration refused");

        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenThrow(enableFailure);

        assertThatThrownBy(() -> triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L))
                .isSameAs(enableFailure);

        verify(triggerDefinitionFacade, never()).executeWebhookDisable(
            anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());
        verify(triggerScheduler, never()).cancelDynamicWebhookTriggerRefresh(anyString());
    }

    @Test
    public void testExecuteTriggerEnableReturnsTheOutputOfTheWebhookSubscription() {
        stubTriggerType(TriggerType.DYNAMIC_WEBHOOK);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "subscription-1"), null);

        when(
            triggerDefinitionFacade.executeWebhookEnable(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), anyString(), anyLong()))
                    .thenReturn(webhookEnableOutput);

        WebhookEnableOutput enableOutput = triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L);

        assertThat(enableOutput).isSameAs(webhookEnableOutput);

        verify(triggerStateService).save(workflowExecutionId, webhookEnableOutput);
    }

    @Test
    public void testExecuteTriggerEnableReturnsNoOutputWhenTheWebhookProviderReturnedNone() {
        stubTriggerType(TriggerType.STATIC_WEBHOOK);

        WebhookEnableOutput enableOutput = triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L);

        assertThat(enableOutput).isNull();

        verify(triggerStateService, never()).save(any(), any());
    }

    @Test
    public void testExecuteTriggerEnableReturnsNoOutputForAListenerTrigger() {
        stubTriggerType(TriggerType.LISTENER);

        WebhookEnableOutput enableOutput = triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, "https://hooks.example/1", 1L);

        assertThat(enableOutput).isNull();

        verify(triggerDefinitionFacade).executeListenerEnable(
            "webhook", 1, "newEvent", Map.of(), workflowExecutionId.toString(), 42L);
    }

    @Test
    public void testExecuteTriggerEnableUndoRemovesTheSubscriptionAndRestoresTheEarlierStateAndItsRefresh() {
        Instant previousWebhookExpirationDate = Instant.parse("2026-11-01T00:00:00Z");
        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), previousWebhookExpirationDate);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-enable"), Instant.parse("2026-12-01T00:00:00Z"));

        boolean undone = triggerLifecycleFacade.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of("event", "trigger_1"), 42L,
            webhookEnableOutput, previousWebhookEnableOutput);

        assertThat(undone).isTrue();

        InOrder inOrder = inOrder(triggerDefinitionFacade, triggerScheduler, triggerStateService);

        inOrder.verify(triggerDefinitionFacade)
            .executeWebhookDisable(
                "webhook", 1, "newEvent", Map.of("event", "trigger_1"), workflowExecutionId.toString(),
                Map.of("id", "subscription-armed-by-the-enable"), 42L);
        inOrder.verify(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        inOrder.verify(triggerStateService)
            .save(workflowExecutionId, previousWebhookEnableOutput);
        inOrder.verify(triggerScheduler)
            .scheduleDynamicWebhookTriggerRefresh(previousWebhookExpirationDate, "webhook", 1, workflowExecutionId,
                42L);
        verify(triggerStateService, never()).fetchValue(any());
        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerEnableUndoRestoresAnEarlierStateReadAsAMap() {
        Map<String, Object> previousTriggerState = Map.of(
            "parameters", Map.of("id", "subscription-armed-before-the-enable"),
            "webhookExpirationDate", "2026-11-01T00:00:00Z");
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-enable"), null);

        boolean undone = triggerLifecycleFacade.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, webhookEnableOutput,
            previousTriggerState);

        assertThat(undone).isTrue();

        verify(triggerStateService).save(
            workflowExecutionId,
            new WebhookEnableOutput(
                Map.of("id", "subscription-armed-before-the-enable"), Instant.parse("2026-11-01T00:00:00Z")));
        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerEnableUndoDeletesTheStateWhenThereWasNoEarlierState() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-enable"), null);

        boolean undone = triggerLifecycleFacade.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), null, webhookEnableOutput, null);

        assertThat(undone).isTrue();

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "webhook", 1, "newEvent", Map.of(), workflowExecutionId.toString(),
            Map.of("id", "subscription-armed-by-the-enable"), null);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verify(triggerStateService).delete(workflowExecutionId);
        verify(triggerStateService, never()).save(any(), any());
        verify(triggerScheduler, never()).scheduleDynamicWebhookTriggerRefresh(any(), any(), anyInt(), any(), any());
    }

    @Test
    public void testExecuteTriggerEnableUndoKeepsTheStateAndReportsASubscriptionThatCouldNotBeRemoved() {
        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), null);
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-enable"), null);

        doThrow(new IllegalStateException("webhook deregistration refused")).when(triggerDefinitionFacade)
            .executeWebhookDisable(anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());

        Logger logger = (Logger) LoggerFactory.getLogger(TriggerLifecycleFacadeImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        boolean undone;

        try {
            undone = triggerLifecycleFacade.executeTriggerEnableUndo(
                "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, webhookEnableOutput,
                previousWebhookEnableOutput);
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(undone).isFalse();

        verify(triggerScheduler, never()).cancelDynamicWebhookTriggerRefresh(anyString());
        verify(triggerStateService, never()).save(any(), any());
        verify(triggerStateService, never()).delete(any());

        assertThat(listAppender.list)
            .anySatisfy(loggingEvent -> {
                assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
                assertThat(loggingEvent.getFormattedMessage()).contains("trigger_1");
                assertThat(loggingEvent.getThrowableProxy()
                    .getMessage()).isEqualTo("webhook deregistration refused");
            });
    }

    @Test
    public void testExecuteTriggerEnableUndoReportsARemovedSubscriptionWhoseEarlierStateCouldNotBeRestored() {
        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-enable"), Instant.parse("2026-11-01T00:00:00Z"));
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-enable"), null);

        doThrow(new IllegalStateException("trigger state restore refused")).when(triggerStateService)
            .save(workflowExecutionId, previousWebhookEnableOutput);

        boolean undone = triggerLifecycleFacade.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WEBHOOK_TRIGGER_TYPE, Map.of(), 42L, webhookEnableOutput,
            previousWebhookEnableOutput);

        assertThat(undone).isTrue();

        verify(triggerScheduler, never()).scheduleDynamicWebhookTriggerRefresh(any(), any(), anyInt(), any(), any());
    }

    private void stubTriggerType(TriggerType triggerType) {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getType()).thenReturn(triggerType);
        when(triggerDefinitionService.getTriggerDefinition("webhook", 1, "newEvent")).thenReturn(triggerDefinition);
    }
}
