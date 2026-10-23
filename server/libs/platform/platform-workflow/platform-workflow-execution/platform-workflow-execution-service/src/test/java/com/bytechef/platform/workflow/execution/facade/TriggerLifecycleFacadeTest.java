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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TriggerLifecycleFacadeTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.EMBEDDED, 1L, "4f1c4b0e-8f53-4c41-9c33-5d4c2b8a1f10", "trigger_1");

    private static final WorkflowNodeType WORKFLOW_NODE_TYPE = WorkflowNodeType.ofType("slack/v1/newMessage");

    private final TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
    private final TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);
    private final TriggerStateService triggerStateService = mock(TriggerStateService.class);

    private final TriggerLifecycleFacade triggerLifecycleFacade = new TriggerLifecycleFacadeImpl(
        triggerScheduler, triggerDefinitionFacade, triggerDefinitionService, triggerStateService);

    @Test
    public void testExecuteTriggerEnableUnregistersTheWebhookWhenSavingItsStateFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(webhookEnableOutput);

        IllegalStateException saveFailure = new IllegalStateException("state save failed");

        doThrow(saveFailure)
            .when(triggerStateService)
            .save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(),
            Map.of("webhookId", "hook-1"), 7L);
    }

    @Test
    public void testExecuteTriggerEnableUnregistersTheWebhookAndDeletesItsStateWhenSchedulingTheRefreshFails() {
        Instant webhookExpirationDate = Instant.parse("2026-11-01T00:00:00Z");

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("webhookId", "hook-1"), webhookExpirationDate);

        mockWebhookEnable(webhookEnableOutput);

        IllegalStateException scheduleFailure = new IllegalStateException("schedule failed");

        doThrow(scheduleFailure)
            .when(triggerScheduler)
            .scheduleDynamicWebhookTriggerRefresh(
                webhookExpirationDate, "slack", 1, WORKFLOW_EXECUTION_ID, 7L);

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(scheduleFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(),
            Map.of("webhookId", "hook-1"), 7L);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(WORKFLOW_EXECUTION_ID.toString());
        verify(triggerStateService).delete(WORKFLOW_EXECUTION_ID);
    }

    @Test
    public void testExecuteTriggerEnableKeepsTheEarlierStateWhenSavingTheNewStateFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-2"), null);

        mockWebhookEnable(webhookEnableOutput);

        when(triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID))
            .thenReturn(Optional.of(new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null)));

        IllegalStateException saveFailure = new IllegalStateException("state save failed");

        doThrow(saveFailure)
            .when(triggerStateService)
            .save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        verify(triggerStateService, never()).delete(any());
    }

    @Test
    public void testExecuteTriggerEnableUnregistersTheWebhookWithEmptyParametersWhenTheOutputHasNone() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(null, null);

        mockWebhookEnable(webhookEnableOutput);

        IllegalStateException saveFailure = new IllegalStateException("state save failed");

        doThrow(saveFailure)
            .when(triggerStateService)
            .save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(), Map.of(), 7L);
    }

    @Test
    public void testExecuteTriggerEnableKeepsTheOriginalFailureWhenCancellingTheRefreshFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(webhookEnableOutput);

        IllegalStateException saveFailure = new IllegalStateException("state save failed");
        IllegalStateException cancelFailure = new IllegalStateException("refresh cancel failed");

        doThrow(saveFailure)
            .when(triggerStateService)
            .save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);
        doThrow(cancelFailure)
            .when(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(WORKFLOW_EXECUTION_ID.toString());

        assertThatThrownBy(this::executeTriggerEnable)
            .isSameAs(saveFailure)
            .hasSuppressedException(cancelFailure);
    }

    @Test
    public void testExecuteTriggerEnableKeepsTheOriginalFailureWhenUnregisteringTheWebhookFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(webhookEnableOutput);

        IllegalStateException saveFailure = new IllegalStateException("state save failed");
        IllegalStateException disableFailure = new IllegalStateException("webhook disable failed");

        doThrow(saveFailure)
            .when(triggerStateService)
            .save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);
        doThrow(disableFailure)
            .when(triggerDefinitionFacade)
            .executeWebhookDisable(anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());

        assertThatThrownBy(this::executeTriggerEnable)
            .isSameAs(saveFailure)
            .hasSuppressedException(disableFailure);
    }

    private void executeTriggerEnable() {
        triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", WORKFLOW_EXECUTION_ID, WORKFLOW_NODE_TYPE, Map.of("channel", "#ops"), 7L,
            "https://example.com/webhooks/1", 1L);
    }

    private void mockWebhookEnable(WebhookEnableOutput webhookEnableOutput) {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getType()).thenReturn(TriggerType.DYNAMIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition("slack", 1, "newMessage"))
            .thenReturn(triggerDefinition);
        when(triggerDefinitionFacade.executeWebhookEnable(
            eq("slack"), eq(1), eq("newMessage"), anyMap(), anyString(), eq(7L), anyString(), anyLong()))
                .thenReturn(webhookEnableOutput);
    }

    @Disabled
    @Test
    public void testPerformTriggerDisable() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformTriggerEnable() {
        // TODO
    }
}
