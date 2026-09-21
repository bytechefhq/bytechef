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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeTestOutputFacadeTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final String WORKFLOW_ID = "workflow1";

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Mock
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeTestOutputFacadeImpl workflowNodeTestOutputFacade;

    @BeforeEach
    void setUp() {
        workflowNodeTestOutputFacade = new WorkflowNodeTestOutputFacadeImpl(
            actionDefinitionFacade, clusterElementDefinitionService, List.of(), connectionService, evaluator,
            jobPrincipalAccessorRegistry, triggerDefinitionFacade, workflowNodeTestOutputService,
            workflowNodeOutputFacade, webhookTriggerTestFacade, workflowService, workflowTestConfigurationService);
    }

    @Test
    void testSaveWorkflowNodeTestOutputExecutesTriggerNamedInWorkflowExecutionId() {
        String workflowUuid = "workflow-uuid-1";
        Workflow workflow = new Workflow(WORKFLOW_ID, """
            {
                "triggers": [
                    {"name": "trigger_1", "type": "manual/v1/manual", "parameters": {}},
                    {"name": "trigger_2", "type": "webhook/v1/autoRespondWithHTTP200", "parameters": {}}
                ],
                "tasks": []
            }
            """, Format.JSON);
        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
        WebhookRequest webhookRequest = mock(WebhookRequest.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getLastWorkflowId(workflowUuid)).thenReturn(WORKFLOW_ID);
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenAnswer(invocation -> Map.of());
        when(evaluator.evaluate(anyMap(), anyMap(), anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, "trigger_2", ENVIRONMENT_ID)).thenReturn(List.of());
        when(triggerDefinitionFacade.executeTrigger(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), isNull(), eq(workflowUuid), isNull(), anyMap(),
            any(), eq(webhookRequest), isNull(), eq(ENVIRONMENT_ID), eq(PlatformType.AUTOMATION), eq(true)))
                .thenReturn(new TriggerOutput(Map.of("body", "ok"), null, false));

        workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            WorkflowExecutionId.of(PlatformType.AUTOMATION, -1, workflowUuid, "trigger_2"), ENVIRONMENT_ID,
            webhookRequest);

        verify(workflowNodeTestOutputService).save(
            eq(WORKFLOW_ID), eq("trigger_2"), any(WorkflowNodeType.class), any(), eq(ENVIRONMENT_ID));
        verify(webhookTriggerTestFacade).disableTrigger(
            WORKFLOW_ID, "trigger_2", ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }
}
