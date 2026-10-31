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

import static com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.test.config.TestWorkflowExecutorIntTestConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs {@link AiAgentTestFacadeImpl} over Postgres with the real {@code WorkflowService},
 * {@code WorkflowTestConfigurationService}, {@code WorkflowNodeOutputFacade} and SpEL {@code Evaluator}. Only
 * {@link ActionDefinitionFacade}, the boundary where the AI component calls the model, is mocked.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = TestWorkflowExecutorIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
class AiAgentTestFacadeIntTest {

    private static final String AI_AGENT_NODE_NAME = "aiAgent1";
    private static final String CONVERSATION_ID = "conv-123";
    private static final long ENVIRONMENT_ID = 10L;
    private static final String MESSAGE = "Hello, AI agent!";
    private static final String PREVIOUS_NODE_NAME = "echo1";

    @MockitoBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private AiAgentTestFacade aiAgentTestFacade;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @Autowired
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @AfterEach
    void afterEach() {
        workflowNodeTestOutputRepository.deleteAll();
        workflowTestConfigurationRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(Objects.requireNonNull(workflow.getId()));
        }
    }

    @Test
    void testExecuteAiAgentActionResolvesParametersAndExecutesAction() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "echo1", "type": "testComponent/v1/echo", "parameters": {"greeting": "hi"}},
                        {
                            "name": "aiAgent1",
                            "type": "aiComponent/v1/chat",
                            "parameters": {"model": "${modelName}", "systemPrompt": "=echo1.greeting"}
                        }
                    ]
                }
                """);

        workflowTestConfigurationService.saveWorkflowTestConfiguration(
            new WorkflowTestConfiguration(
                ENVIRONMENT_ID, Map.of("modelName", "gpt-4"), workflowId,
                List.of(new WorkflowTestConfigurationConnection(42L, "apiKey", AI_AGENT_NODE_NAME))));

        savePreviousNodeOutput(workflowId, Map.of("greeting", "Good morning"));

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), isNull(), eq(workflowId), any(),
                any(), any(), eq(ENVIRONMENT_ID), isNull(), eq(true), isNull(), isNull(), isNull()))
                    .thenReturn("AI response");

        Object result = aiAgentTestFacade.executeAiAgentAction(
            workflowId, AI_AGENT_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        assertThat(result).isEqualTo("AI response");

        ExecutePerformArguments executePerformArguments = captureExecutePerform(workflowId);

        assertThat(executePerformArguments.parameters())
            .containsEntry("model", "gpt-4")
            .containsEntry("systemPrompt", "Good morning");
        assertThat(executePerformArguments.connectionIds()).isEqualTo(Map.of("apiKey", 42L));
    }

    @Test
    void testExecuteAiAgentActionSetsConversationIdAndUserPrompt() {
        String workflowId = createWorkflow(getAiAgentWorkflowDefinition("{}", ""));

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyBoolean(), any(), any(), any())).thenReturn("result");

        aiAgentTestFacade.executeAiAgentAction(
            workflowId, AI_AGENT_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        ExecutePerformArguments executePerformArguments = captureExecutePerform(workflowId);

        assertThat(executePerformArguments.parameters())
            .containsEntry("conversationId", CONVERSATION_ID)
            .containsEntry("userPrompt", MESSAGE)
            .containsEntry("attachments", List.of());
    }

    @Test
    void testExecuteAiAgentActionThrowsWhenWorkflowTaskNotFound() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "differentNode", "type": "aiComponent/v1/chat", "parameters": {}}
                    ]
                }
                """);

        assertThatThrownBy(
            () -> aiAgentTestFacade.executeAiAgentAction(
                workflowId, AI_AGENT_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(AI_AGENT_NODE_NAME);

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testExecuteAiAgentActionEvaluatesExtensions() {
        String workflowId =
            createWorkflow(getAiAgentWorkflowDefinition("{}", ", \"clusterElements\": {\"tools\": \"${toolsList}\"}"));

        workflowTestConfigurationService.saveWorkflowTestConfiguration(
            new WorkflowTestConfiguration(
                ENVIRONMENT_ID, Map.of("toolsList", List.of("tool1", "tool2")), workflowId, List.of()));

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyBoolean(), any(), any(), any())).thenReturn("result");

        aiAgentTestFacade.executeAiAgentAction(
            workflowId, AI_AGENT_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        ExecutePerformArguments executePerformArguments = captureExecutePerform(workflowId);

        assertThat(executePerformArguments.extensions())
            .containsEntry("clusterElements", Map.of("tools", List.of("tool1", "tool2")));
    }

    @Test
    void testExecuteAiAgentActionResolvesConnectionIds() {
        String workflowId = createWorkflow(getAiAgentWorkflowDefinition("{}", ""));

        workflowTestConfigurationService.saveWorkflowTestConfiguration(
            new WorkflowTestConfiguration(
                ENVIRONMENT_ID, Map.of(), workflowId,
                List.of(
                    new WorkflowTestConfigurationConnection(100L, "openaiKey", AI_AGENT_NODE_NAME),
                    new WorkflowTestConfigurationConnection(200L, "anthropicKey", AI_AGENT_NODE_NAME),
                    new WorkflowTestConfigurationConnection(300L, "otherKey", PREVIOUS_NODE_NAME))));

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyBoolean(), any(), any(), any())).thenReturn("result");

        aiAgentTestFacade.executeAiAgentAction(
            workflowId, AI_AGENT_NODE_NAME, ENVIRONMENT_ID, CONVERSATION_ID, MESSAGE, List.of());

        ExecutePerformArguments executePerformArguments = captureExecutePerform(workflowId);

        assertThat(executePerformArguments.connectionIds())
            .isEqualTo(Map.of("openaiKey", 100L, "anthropicKey", 200L));
    }

    @SuppressWarnings("unchecked")
    private ExecutePerformArguments captureExecutePerform(String workflowId) {
        ArgumentCaptor<Map<String, ?>> parametersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Long>> connectionIdsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, ?>> extensionsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionFacade).executePerform(
            eq("aiComponent"), eq(1), eq("chat"), isNull(), isNull(), isNull(), isNull(), eq(workflowId),
            parametersCaptor.capture(), connectionIdsCaptor.capture(), extensionsCaptor.capture(), eq(ENVIRONMENT_ID),
            isNull(), eq(true), isNull(), isNull(), isNull());

        return new ExecutePerformArguments(
            (Map<String, Object>) parametersCaptor.getValue(), connectionIdsCaptor.getValue(),
            (Map<String, Object>) extensionsCaptor.getValue());
    }

    private String createWorkflow(String definition) {
        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return Objects.requireNonNull(workflow.getId());
    }

    private static String getAiAgentWorkflowDefinition(String parameters, String extensions) {
        return """
            {
                "tasks": [
                    {"name": "aiAgent1", "type": "aiComponent/v1/chat", "parameters": %s%s}
                ]
            }
            """.formatted(parameters, extensions);
    }

    private void savePreviousNodeOutput(String workflowId, Map<String, ?> sampleOutput) {
        workflowNodeTestOutputService.save(
            workflowId, PREVIOUS_NODE_NAME, WorkflowNodeType.ofType(COMPONENT_NAME + "/v1/echo"),
            new OutputResponse(new StringProperty(ComponentDsl.string("sample")), sampleOutput), false,
            ENVIRONMENT_ID);
    }

    private record ExecutePerformArguments(
        Map<String, Object> parameters, Map<String, Long> connectionIds, Map<String, Object> extensions) {
    }
}
