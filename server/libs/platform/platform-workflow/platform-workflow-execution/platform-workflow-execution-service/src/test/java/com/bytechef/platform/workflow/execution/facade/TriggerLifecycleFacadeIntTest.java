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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.TriggerState;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@Import(TriggerLifecycleFacadeImpl.class)
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class TriggerLifecycleFacadeIntTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.EMBEDDED, 1L, "4f1c4b0e-8f53-4c41-9c33-5d4c2b8a1f10", "trigger_1");

    private static final WorkflowNodeType WORKFLOW_NODE_TYPE = WorkflowNodeType.ofType("slack/v1/newMessage");

    @MockitoBean
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @MockitoBean
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockitoBean
    private TriggerScheduler triggerScheduler;

    @MockitoSpyBean
    private TriggerStateRepository triggerStateRepository;

    @Autowired
    private TriggerStateService triggerStateService;

    @AfterEach
    void afterEach() {
        triggerStateRepository.deleteAll();
    }

    @Test
    void testExecuteTriggerEnableUnregistersTheWebhookWhenSavingItsStateFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        IllegalStateException saveFailure = failTriggerStateSave();

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(),
            Map.of("webhookId", "hook-1"), 7L);

        assertThat(triggerStateRepository.count()).isZero();
    }

    @Test
    void testExecuteTriggerEnableUnregistersTheWebhookAndDeletesItsStateWhenSchedulingTheRefreshFails() {
        Instant webhookExpirationDate = Instant.parse("2026-11-01T00:00:00Z");

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("webhookId", "hook-1"), webhookExpirationDate);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

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

        assertThat(triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID)).isEmpty();
        assertThat(triggerStateRepository.count()).isZero();
    }

    @Test
    void testExecuteTriggerEnableKeepsTheEarlierStateWhenSavingTheNewStateFails() {
        triggerStateService.save(
            WORKFLOW_EXECUTION_ID, new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null));

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-2"), null);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        IllegalStateException saveFailure = failTriggerStateSave();

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        Optional<WebhookEnableOutput> storedOutput = triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID);

        assertThat(storedOutput).hasValue(new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null));
    }

    @Test
    void testExecuteTriggerEnableUnregistersTheWebhookWithEmptyParametersWhenTheOutputHasNone() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(null, null);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        IllegalStateException saveFailure = failTriggerStateSave();

        assertThatThrownBy(this::executeTriggerEnable).isSameAs(saveFailure);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(), Map.of(), 7L);
    }

    @Test
    void testExecuteTriggerEnableKeepsTheOriginalFailureWhenCancellingTheRefreshFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        IllegalStateException saveFailure = failTriggerStateSave();
        IllegalStateException cancelFailure = new IllegalStateException("refresh cancel failed");

        doThrow(cancelFailure)
            .when(triggerScheduler)
            .cancelDynamicWebhookTriggerRefresh(WORKFLOW_EXECUTION_ID.toString());

        assertThatThrownBy(this::executeTriggerEnable)
            .isSameAs(saveFailure)
            .hasSuppressedException(cancelFailure);
    }

    @Test
    void testExecuteTriggerEnableKeepsTheOriginalFailureWhenUnregisteringTheWebhookFails() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        IllegalStateException saveFailure = failTriggerStateSave();
        IllegalStateException disableFailure = new IllegalStateException("webhook disable failed");

        doThrow(disableFailure)
            .when(triggerDefinitionFacade)
            .executeWebhookDisable(anyString(), anyInt(), anyString(), anyMap(), anyString(), anyMap(), any());

        assertThatThrownBy(this::executeTriggerEnable)
            .isSameAs(saveFailure)
            .hasSuppressedException(disableFailure);
    }

    @Test
    void testPerformTriggerEnable() {
        Instant webhookExpirationDate = Instant.parse("2026-11-01T00:00:00Z");

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("webhookId", "hook-1"), webhookExpirationDate);

        mockWebhookEnable(TriggerType.DYNAMIC_WEBHOOK, webhookEnableOutput);

        executeTriggerEnable();

        Optional<WebhookEnableOutput> storedOutput = triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID);

        assertThat(storedOutput).hasValue(webhookEnableOutput);

        verify(triggerScheduler).scheduleDynamicWebhookTriggerRefresh(
            webhookExpirationDate, "slack", 1, WORKFLOW_EXECUTION_ID, 7L);
    }

    @Test
    void testPerformTriggerDisable() {
        triggerStateService.save(
            WORKFLOW_EXECUTION_ID, new WebhookEnableOutput(Map.of("webhookId", "hook-1"), null));

        mockTriggerDefinition(TriggerType.DYNAMIC_WEBHOOK);

        triggerLifecycleFacade.executeTriggerDisable(
            "workflow-1", WORKFLOW_EXECUTION_ID, WORKFLOW_NODE_TYPE, Map.of("channel", "#ops"), 7L);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "slack", 1, "newMessage", Map.of("channel", "#ops"), WORKFLOW_EXECUTION_ID.toString(),
            Map.of("webhookId", "hook-1"), 7L);
        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(WORKFLOW_EXECUTION_ID.toString());

        assertThat(triggerStateRepository.count()).isZero();
    }

    private void executeTriggerEnable() {
        triggerLifecycleFacade.executeTriggerEnable(
            "workflow-1", WORKFLOW_EXECUTION_ID, WORKFLOW_NODE_TYPE, Map.of("channel", "#ops"), 7L,
            "https://example.com/webhooks/1", 1L);
    }

    private IllegalStateException failTriggerStateSave() {
        IllegalStateException saveFailure = new IllegalStateException("state save failed");

        doThrow(saveFailure)
            .when(triggerStateRepository)
            .save(any(TriggerState.class));

        return saveFailure;
    }

    private void mockTriggerDefinition(TriggerType triggerType) {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getType()).thenReturn(triggerType);
        when(triggerDefinitionService.getTriggerDefinition("slack", 1, "newMessage"))
            .thenReturn(triggerDefinition);
    }

    private void mockWebhookEnable(TriggerType triggerType, WebhookEnableOutput webhookEnableOutput) {
        mockTriggerDefinition(triggerType);

        when(triggerDefinitionFacade.executeWebhookEnable(
            eq("slack"), eq(1), eq("newMessage"), anyMap(), anyString(), eq(7L), anyString(), anyLong()))
                .thenReturn(webhookEnableOutput);
    }
}
