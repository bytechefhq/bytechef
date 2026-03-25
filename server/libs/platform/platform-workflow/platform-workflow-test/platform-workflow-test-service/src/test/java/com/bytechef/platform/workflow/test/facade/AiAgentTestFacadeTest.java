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

package com.bytechef.platform.workflow.test.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class AiAgentTestFacadeTest {

    private static final String WORKFLOW_ID = "wf-ai-1";
    private static final String WORKFLOW_NODE_NAME = "aiAgent1";
    private static final long ENVIRONMENT_ID = 10L;
    private static final String CONVERSATION_ID = "conv-123";
    private static final String MESSAGE = "Hello, AI agent!";

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private Evaluator evaluator;

    @Mock
    private TempFileStorage tempFileStorage;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private AiAgentTestFacadeImpl aiAgentTestFacade;

    @BeforeEach
    void beforeEach() {
        aiAgentTestFacade = new AiAgentTestFacadeImpl(
            actionDefinitionFacade, evaluator, tempFileStorage, workflowNodeOutputFacade, workflowService,
            workflowTestConfigurationService);
    }

    @Test
    void executeAiAgentActionResolvesParametersAndExecutesAction() {
        // Given a workflow with one matching task
        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of(workflowTask));
        when(workflowTask.getName()).thenReturn(WORKFLOW_NODE_NAME);
        when(workflowTask.getType()).thenReturn("aiComponent/v1/chat");
        doReturn(Map.of("model", "gpt-4")).when(workflowTask)
            .getParameters();
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        // And test configuration inputs and outputs
        Map<String, Object> inputs = Map.of("inputKey", "inputValue");
        Map<String, Object> outputs = Map.of("outputKey", "outputValue");

        doReturn(inputs).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID);
        doReturn(outputs).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID);

        // And the evaluator returns parameters without attachments
        Map<String, Object> evaluatedParameters = new HashMap<>(
            Map.of("model", "gpt-4", "conversationId", CONVERSATION_ID, "userPrompt", MESSAGE));

        when(evaluator.evaluate(any(), any())).thenReturn(evaluatedParameters);

        // And one connection mapping
        WorkflowTestConfigurationConnection connection = new WorkflowTestConfigurationConnection(
            42L, "apiKey", WORKFLOW_NODE_NAME);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID)).thenReturn(List.of(connection));

        // And the action returns a result
        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), isNull(), eq(WORKFLOW_ID), any(),
            any(), any(), eq(ENVIRONMENT_ID), isNull(), eq(true), isNull(), isNull(), isNull()))
                .thenReturn("AI response");

        // When
        Object result = aiAgentTestFacade.executeAiAgentAction(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        // Then
        assertThat(result).isEqualTo("AI response");

        verify(actionDefinitionFacade).executePerform(
            eq("aiComponent"), eq(1), eq("chat"), isNull(), isNull(), isNull(), isNull(), eq(WORKFLOW_ID), any(),
            any(), any(), eq(ENVIRONMENT_ID), isNull(), eq(true), isNull(), isNull(), isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    void executeAiAgentActionSetsConversationIdAndUserPrompt() {
        // Given a workflow with one matching task
        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of(workflowTask));
        when(workflowTask.getName()).thenReturn(WORKFLOW_NODE_NAME);
        when(workflowTask.getType()).thenReturn("aiComponent/v1/chat");
        doReturn(Map.of()).when(workflowTask)
            .getParameters();
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID);
        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID);

        // Capture the parameters passed to evaluate to verify conversationId and userPrompt were set
        ArgumentCaptor<Map<String, ?>> parametersCaptor = ArgumentCaptor.forClass(Map.class);

        when(evaluator.evaluate(parametersCaptor.capture(), any())).thenReturn(new HashMap<>());
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID)).thenReturn(List.of());
        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(boolean.class), any(), any(), any())).thenReturn("result");

        // When
        aiAgentTestFacade.executeAiAgentAction(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        // Then the first evaluate call should have conversationId and userPrompt
        Map<String, Object> capturedParameters = (Map<String, Object>) parametersCaptor.getAllValues()
            .getFirst();

        assertThat(capturedParameters).containsEntry("conversationId", CONVERSATION_ID);
        assertThat(capturedParameters).containsEntry("userPrompt", MESSAGE);
        assertThat(capturedParameters).containsKey("attachments");
    }

    @Test
    void executeAiAgentActionThrowsWhenWorkflowTaskNotFound() {
        // Given a workflow with no matching task
        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of(workflowTask));
        when(workflowTask.getName()).thenReturn("differentNode");

        // When / Then
        assertThatThrownBy(
            () -> aiAgentTestFacade.executeAiAgentAction(
                WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(WORKFLOW_NODE_NAME);
    }

    @Test
    @SuppressWarnings("unchecked")
    void executeAiAgentActionEvaluatesExtensions() {
        // Given a workflow with extensions
        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of(workflowTask));
        when(workflowTask.getName()).thenReturn(WORKFLOW_NODE_NAME);
        when(workflowTask.getType()).thenReturn("aiComponent/v1/chat");
        when(workflowTask.getParameters()).thenReturn(Map.of());
        doReturn(Map.of("tools", "${toolsList}")).when(workflowTask)
            .getExtensions();

        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID);
        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID);

        Map<String, Object> evaluatedExtensions = Map.of("tools", List.of("tool1", "tool2"));

        // First call is for parameters, second for extensions
        when(evaluator.evaluate(any(), any()))
            .thenReturn(new HashMap<>())
            .thenReturn(evaluatedExtensions);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID)).thenReturn(List.of());

        ArgumentCaptor<Map<String, ?>> extensionsCaptor = ArgumentCaptor.forClass(Map.class);

        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(),
            extensionsCaptor.capture(), any(), any(), any(boolean.class), any(), any(), any())).thenReturn("result");

        // When
        aiAgentTestFacade.executeAiAgentAction(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        // Then extensions are evaluated and passed to executePerform
        Map<String, Object> capturedExtensions = (Map<String, Object>) extensionsCaptor.getValue();

        assertThat(capturedExtensions).containsEntry("tools", List.of("tool1", "tool2"));
    }

    @Test
    void executeAiAgentActionResolvesConnectionIds() {
        // Given a workflow task with connections
        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of(workflowTask));
        when(workflowTask.getName()).thenReturn(WORKFLOW_NODE_NAME);
        when(workflowTask.getType()).thenReturn("aiComponent/v1/chat");
        doReturn(Map.of()).when(workflowTask)
            .getParameters();
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID);
        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID);

        when(evaluator.evaluate(any(), any())).thenReturn(new HashMap<>());

        // And two connection mappings
        WorkflowTestConfigurationConnection connection1 = new WorkflowTestConfigurationConnection(
            100L, "openaiKey", WORKFLOW_NODE_NAME);
        WorkflowTestConfigurationConnection connection2 = new WorkflowTestConfigurationConnection(
            200L, "anthropicKey", WORKFLOW_NODE_NAME);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID)).thenReturn(List.of(connection1, connection2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Long>> connectionIdsCaptor = ArgumentCaptor.forClass(Map.class);

        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), connectionIdsCaptor.capture(),
            any(), any(), any(), any(boolean.class), any(), any(), any())).thenReturn("result");

        // When
        aiAgentTestFacade.executeAiAgentAction(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        // Then connection IDs are resolved and passed
        Map<String, Long> capturedConnectionIds = connectionIdsCaptor.getValue();

        assertThat(capturedConnectionIds).containsEntry("openaiKey", 100L);
        assertThat(capturedConnectionIds).containsEntry("anthropicKey", 200L);
    }
}
