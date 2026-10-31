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
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pins that the webhook test URL is minted for the trigger the editor asked for, not for the workflow's first trigger —
 * a workflow with a manual trigger ahead of a webhook trigger used to get a test URL naming the manual one.
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WebhookTriggerTestFacadeIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final long ENVIRONMENT_ID = 0L;
    private static final String MANUAL_TRIGGER_NAME = "trigger_1";
    private static final String WEBHOOK_TRIGGER_NAME = "trigger_2";
    private static final String WEBHOOK_URL_PREFIX = "http://localhost:9555/webhooks/";

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

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private String workflowId;
    private String workflowUuid;

    @BeforeEach
    void beforeEach() {
        Workflow workflow = workflowService.create(DEFINITION, Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();
        workflowUuid = "workflow-uuid-" + workflowId;

        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowUuid(workflowId)).thenReturn(workflowUuid);
        when(jobPrincipalAccessor.getLastWorkflowId(workflowUuid)).thenReturn(workflowId);

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
    }

    @AfterEach
    void afterEach() {
        workflowTestConfigurationRepository.deleteAll();
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testEnableTriggerMintsUrlForRequestedTrigger() {
        String webhookUrl = webhookTriggerTestFacade.enableTrigger(
            workflowId, WEBHOOK_TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        WorkflowExecutionId workflowExecutionId = parseWorkflowExecutionId(webhookUrl);

        assertThat(workflowExecutionId.getTriggerName()).isEqualTo(WEBHOOK_TRIGGER_NAME);
        assertThat(workflowExecutionId.getWorkflowUuid()).isEqualTo(workflowUuid);
        assertThat(webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId)).isTrue();

        verify(triggerDefinitionFacade).executeWebhookEnable(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), anyMap(), eq(workflowExecutionId.toString()),
            isNull(), eq(webhookUrl), eq(ENVIRONMENT_ID));
        verify(triggerDefinitionFacade, never()).executeWebhookEnable(
            eq("manual"), eq(1), eq("manual"), anyMap(), anyString(), isNull(), anyString(), eq(ENVIRONMENT_ID));
    }

    @Test
    void testEnableTriggerWithoutTriggerNameFallsBackToFirstTrigger() {
        String webhookUrl = webhookTriggerTestFacade.enableTrigger(
            workflowId, null, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        assertThat(parseWorkflowExecutionId(webhookUrl).getTriggerName()).isEqualTo(MANUAL_TRIGGER_NAME);
    }

    @Test
    void testDisableTriggerTearsDownRequestedTrigger() {
        webhookTriggerTestFacade.disableTrigger(
            workflowId, WEBHOOK_TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.AUTOMATION);

        verify(triggerDefinitionService).getTriggerDefinition("webhook", 1, "autoRespondWithHTTP200");
        verify(triggerDefinitionService, never()).getTriggerDefinition("manual", 1, "manual");
    }

    @Test
    void testValidateOnEnableUsesTriggerNamedInWorkflowExecutionId() {
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, WEBHOOK_TRIGGER_NAME, "connection", CONNECTION_ID, true, ENVIRONMENT_ID);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, -1, workflowUuid, WEBHOOK_TRIGGER_NAME);
        WebhookRequest webhookRequest = mock(WebhookRequest.class);

        webhookTriggerTestFacade.validateOnEnable(workflowExecutionId, webhookRequest, ENVIRONMENT_ID);

        verify(triggerDefinitionFacade).executeWebhookValidateOnEnable(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), anyMap(), eq(webhookRequest), eq(CONNECTION_ID));

        assertThat(webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId)).isTrue();
    }

    @Test
    void testValidateUsesTriggerNamedInWorkflowExecutionIdWithoutEnablingWorkflow() {
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, WEBHOOK_TRIGGER_NAME, "connection", CONNECTION_ID, true, ENVIRONMENT_ID);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, -1, workflowUuid, WEBHOOK_TRIGGER_NAME);
        WebhookRequest webhookRequest = mock(WebhookRequest.class);
        WebhookValidateResponse badRequestResponse = WebhookValidateResponse.badRequest();

        when(triggerDefinitionFacade.executeWebhookValidate(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), anyMap(), eq(webhookRequest), eq(CONNECTION_ID)))
                .thenReturn(badRequestResponse);

        assertThat(webhookTriggerTestFacade.validate(workflowExecutionId, webhookRequest, ENVIRONMENT_ID))
            .isSameAs(badRequestResponse);
        assertThat(webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId)).isFalse();
    }

    private static WorkflowExecutionId parseWorkflowExecutionId(String webhookUrl) {
        String suffix = "/test/environments/" + ENVIRONMENT_ID;

        assertThat(webhookUrl).startsWith(WEBHOOK_URL_PREFIX)
            .endsWith(suffix);

        return WorkflowExecutionId.parse(
            webhookUrl.substring(WEBHOOK_URL_PREFIX.length(), webhookUrl.length() - suffix.length()));
    }
}
