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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Pins that the webhook test URL is minted for the trigger the editor asked for, not for the workflow's first trigger —
 * a workflow with a manual trigger ahead of a webhook trigger used to get a test URL naming the manual one.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookTriggerTestFacadeTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final String MANUAL_TRIGGER_NAME = "trigger_1";
    private static final String WEBHOOK_TRIGGER_NAME = "trigger_2";
    private static final String WORKFLOW_ID = "workflow1";
    private static final String WORKFLOW_UUID = "workflow-uuid-1";

    private static final String DEFINITION = """
        {
            "triggers": [
                {
                    "name": "trigger_1",
                    "type": "manual/v1/manual",
                    "parameters": {}
                },
                {
                    "name": "trigger_2",
                    "type": "webhook/v1/autoRespondWithHTTP200",
                    "parameters": {}
                }
            ],
            "tasks": []
        }
        """;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private Evaluator evaluator;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WebhookTriggerTestFacadeImpl webhookTriggerTestFacade;

    @BeforeEach
    void setUp() {
        when(applicationProperties.getWebhookUrl()).thenReturn("http://localhost/webhooks/{id}");
        when(evaluator.evaluate(anyMap(), anyMap(), anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowUuid(WORKFLOW_ID)).thenReturn(WORKFLOW_UUID);
        when(jobPrincipalAccessor.getLastWorkflowId(WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(new Workflow(WORKFLOW_ID, DEFINITION, Format.JSON));
        when(workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            anyString(), anyString(), eq(ENVIRONMENT_ID))).thenReturn(Optional.empty());

        TriggerDefinition manualTriggerDefinition = mock(TriggerDefinition.class);

        when(manualTriggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition("manual", 1, "manual"))
            .thenReturn(manualTriggerDefinition);

        TriggerDefinition webhookTriggerDefinition = mock(TriggerDefinition.class);

        when(webhookTriggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition("webhook", 1, "autoRespondWithHTTP200"))
            .thenReturn(webhookTriggerDefinition);
        when(triggerDefinitionService.getWebhookTriggerFlags(anyString(), eq(1), anyString()))
            .thenReturn(new WebhookTriggerFlags(false, false, false, false));

        webhookTriggerTestFacade = new WebhookTriggerTestFacadeImpl(
            new ConcurrentMapCacheManager(), evaluator, applicationProperties, jobPrincipalAccessorRegistry,
            triggerDefinitionFacade, triggerDefinitionService, workflowService, workflowTestConfigurationService);
    }

    @Test
    void testEnableTriggerMintsUrlForRequestedTrigger() {
        String webhookUrl = webhookTriggerTestFacade.enableTrigger(
            WORKFLOW_ID, WEBHOOK_TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        WorkflowExecutionId workflowExecutionId = parseWorkflowExecutionId(webhookUrl);

        assertThat(workflowExecutionId.getTriggerName()).isEqualTo(WEBHOOK_TRIGGER_NAME);
        assertThat(workflowExecutionId.getWorkflowUuid()).isEqualTo(WORKFLOW_UUID);

        verify(triggerDefinitionFacade).executeWebhookEnable(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), anyMap(), eq(workflowExecutionId.toString()),
            isNull(), eq(webhookUrl), eq(ENVIRONMENT_ID));
        verify(triggerDefinitionFacade, never()).executeWebhookEnable(
            eq("manual"), eq(1), eq("manual"), anyMap(), anyString(), isNull(), anyString(), eq(ENVIRONMENT_ID));
    }

    @Test
    void testEnableTriggerWithoutTriggerNameFallsBackToFirstTrigger() {
        String webhookUrl = webhookTriggerTestFacade.enableTrigger(
            WORKFLOW_ID, null, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        assertThat(parseWorkflowExecutionId(webhookUrl).getTriggerName()).isEqualTo(MANUAL_TRIGGER_NAME);
    }

    @Test
    void testDisableTriggerTearsDownRequestedTrigger() {
        webhookTriggerTestFacade.disableTrigger(
            WORKFLOW_ID, WEBHOOK_TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        verify(triggerDefinitionService).getTriggerDefinition("webhook", 1, "autoRespondWithHTTP200");
        verify(triggerDefinitionService, never()).getTriggerDefinition("manual", 1, "manual");
    }

    @Test
    void testValidateOnEnableUsesTriggerNamedInWorkflowExecutionId() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, -1, WORKFLOW_UUID, WEBHOOK_TRIGGER_NAME);
        WebhookRequest webhookRequest = mock(WebhookRequest.class);

        webhookTriggerTestFacade.validateOnEnable(workflowExecutionId, webhookRequest, ENVIRONMENT_ID);

        verify(workflowTestConfigurationService).fetchWorkflowTestConfigurationConnectionId(
            WORKFLOW_ID, WEBHOOK_TRIGGER_NAME, ENVIRONMENT_ID);
        verify(triggerDefinitionFacade).executeWebhookValidateOnEnable(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), anyMap(), eq(webhookRequest), isNull());
    }

    private static WorkflowExecutionId parseWorkflowExecutionId(String webhookUrl) {
        String prefix = "http://localhost/webhooks/";
        String suffix = "/test/environments/" + ENVIRONMENT_ID;

        assertThat(webhookUrl).startsWith(prefix)
            .endsWith(suffix);

        return WorkflowExecutionId.parse(webhookUrl.substring(prefix.length(), webhookUrl.length() - suffix.length()));
    }
}
