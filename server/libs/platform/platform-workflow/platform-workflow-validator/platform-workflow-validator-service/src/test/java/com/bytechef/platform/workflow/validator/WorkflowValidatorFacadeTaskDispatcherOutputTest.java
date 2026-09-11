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

package com.bytechef.platform.workflow.validator;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorFacadeTaskDispatcherOutputTest {

    private static final String WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {
                        "items": ["ee"],
                        "iteratee": [
                            {
                                "label": "Var",
                                "name": "var_1",
                                "type": "var/v1/set",
                                "parameters": {"type": "STRING", "value": "${map_1.item}"}
                            }
                        ]
                    }
                },
                {
                    "label": "Subflow",
                    "name": "subflow_1",
                    "type": "subflow/v1",
                    "parameters": {"workflowUuid": "25327c41"}
                },
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${subflow_1.message}"}
                }
            ]
        }
        """;

    private static final String HTTP_CLIENT_WORKFLOW = """
        {
            "label": "Learn ByteChef by doing",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Get a random quote",
                    "name": "httpClient_1",
                    "type": "httpClient/v1/get",
                    "parameters": {"uri": "https://dummyjson.com/quotes/random"}
                },
                {
                    "label": "Log the quote",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${httpClient_1.quote}"}
                }
            ]
        }
        """;

    private static final String CONNECTION_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Affinity",
                    "name": "affinity_1",
                    "type": "affinity/v1/createOpportunity",
                    "parameters": {}
                }
            ]
        }
        """;

    private static final String AGENT_WORKFLOW = """
        {
            "label": "Build your first agent",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Explain a quote",
                    "name": "aiAgent_1",
                    "type": "aiAgent/v1/chat",
                    "parameters": {"userPrompt": "Fetch a random quote and explain it."},
                    "clusterElements": {
                        "model": {
                            "label": "OpenAI",
                            "name": "openAi_1",
                            "type": "openAi/v1/model",
                            "parameters": {"model": "gpt-5-mini"}
                        },
                        "tools": [
                            {
                                "label": "Get a random quote",
                                "name": "httpClient_1",
                                "type": "httpClient/v1/get",
                                "parameters": {"uri": "https://dummyjson.com/quotes/random"}
                            }
                        ]
                    }
                }
            ]
        }
        """;

    private static final String BRANCH_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Branch",
                    "name": "branch_1",
                    "type": "branch/v1",
                    "parameters": {
                        "expression": "A",
                        "cases": [
                            {
                                "key": "A",
                                "tasks": [
                                    {
                                        "label": "Affinity",
                                        "name": "affinity_1",
                                        "type": "affinity/v1/createOpportunity",
                                        "parameters": {}
                                    }
                                ]
                            }
                        ],
                        "default": []
                    }
                }
            ]
        }
        """;

    private static final String NESTED_AGENT_WORKFLOW = """
        {
            "label": "Nested agents",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Outer agent",
                    "name": "aiAgent_1",
                    "type": "aiAgent/v1/chat",
                    "parameters": {"userPrompt": "hi"},
                    "clusterElements": {
                        "model": {"name": "openAi_1", "type": "openAi/v1/model", "parameters": {}},
                        "tools": [
                            {
                                "label": "Inner agent",
                                "name": "aiAgent_2",
                                "type": "aiAgent/v1/chat",
                                "parameters": {"userPrompt": "hi"},
                                "clusterElements": {
                                    "model": {"name": "openAi_2", "type": "openAi/v1/model", "parameters": {}}
                                }
                            }
                        ]
                    }
                }
            ]
        }
        """;

    private static final OutputResponse SUBFLOW_OUTPUT = new OutputResponse(
        new ObjectProperty(object("subflow_1").properties(string("message"))), null);

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService =
        mock(TaskDispatcherDefinitionService.class);
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService =
        mock(WorkflowNodeTestOutputService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    private final WorkflowValidatorFacadeImpl workflowValidatorFacade = new WorkflowValidatorFacadeImpl(
        mock(ActionDefinitionFacade.class), actionDefinitionService, mock(ClusterElementDefinitionService.class),
        componentDefinitionService, taskDispatcherDefinitionService, mock(TriggerDefinitionFacade.class),
        mock(TriggerDefinitionService.class), workflowNodeTestOutputService, workflowService,
        workflowTestConfigurationService, List.of());

    @BeforeEach
    void beforeEach() {
        ActionDefinition varActionDefinition = actionDefinition(
            ComponentDsl.string("type"), ComponentDsl.string("value"));
        ActionDefinition loggerActionDefinition = actionDefinition(ComponentDsl.string("text"));

        when(actionDefinitionService.getActionDefinition("var", 1, "set")).thenReturn(varActionDefinition);
        when(actionDefinitionService.getActionDefinition("logger", 1, "info")).thenReturn(loggerActionDefinition);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("map", 1))
            .thenReturn(new TaskDispatcherDefinition("map"));
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("subflow", 1))
            .thenReturn(new TaskDispatcherDefinition("subflow"));
        when(taskDispatcherDefinitionService.executeVariableProperties(eq("map"), eq(1), anyMap()))
            .thenReturn(new OutputResponse(
                new ObjectProperty(object("map_1").properties(string("item"), integer("index"))), null));
    }

    @Test
    void taskDispatcherVariablePropertiesAndDynamicOutputsSatisfyTheReferences() {
        when(taskDispatcherDefinitionService.executeOutput(
            eq("subflow"), eq(1),
            argThat(inputParameters -> Objects.equals(inputParameters.get(MapDataSource.ENVIRONMENT_ID), 3L) &&
                !inputParameters.containsKey(MapDataSource.WORKFLOW_ID))))
                    .thenReturn(SUBFLOW_OUTPUT);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(WORKFLOW, 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void validatingByIdPassesTheWorkflowIdToTheTaskDispatcherOutput() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn(WORKFLOW);
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(taskDispatcherDefinitionService.executeOutput(
            eq("subflow"), eq(1),
            argThat(inputParameters -> Objects.equals(inputParameters.get(MapDataSource.WORKFLOW_ID), "wf-1") &&
                Objects.equals(inputParameters.get(MapDataSource.ENVIRONMENT_ID), 3L))))
                    .thenReturn(SUBFLOW_OUTPUT);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflowById("wf-1", 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void anEmptyTaskDispatcherOutputLeavesTheReferenceUnresolved() {
        when(taskDispatcherDefinitionService.executeOutput(eq("subflow"), eq(1), anyMap()))
            .thenReturn(new OutputResponse(new ObjectProperty(object("subflow_1")), Map.of()));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(WORKFLOW, 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(
            List.of("[logger_1] Property 'subflow_1.message' might not exist in the output of 'subflow/v1'"),
            result.warnings());
    }

    @Test
    void theNodeTestOutputSatisfiesTheReferenceWhenTheWorkflowIdIsKnown() {
        ActionDefinition httpClientActionDefinition = actionDefinition(ComponentDsl.string("uri"));
        WorkflowNodeTestOutput workflowNodeTestOutput = mock(WorkflowNodeTestOutput.class);

        when(actionDefinitionService.getActionDefinition("httpClient", 1, "get"))
            .thenReturn(httpClientActionDefinition);
        stubStoredType(workflowNodeTestOutput, "get");
        when(workflowNodeTestOutput.getOutput(any())).thenReturn(new OutputResponse(
            Property.toProperty(ComponentDsl.object("httpClient_1")
                .properties(ComponentDsl.string("quote"), ComponentDsl.string("author"))),
            null));
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput("wf-1", "httpClient_1", 3L))
            .thenReturn(Optional.of(workflowNodeTestOutput));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(HTTP_CLIENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void aPropertyMissingFromTheNodeTestOutputIsAnError() {
        ActionDefinition httpClientActionDefinition = actionDefinition(ComponentDsl.string("uri"));
        WorkflowNodeTestOutput workflowNodeTestOutput = mock(WorkflowNodeTestOutput.class);

        when(actionDefinitionService.getActionDefinition("httpClient", 1, "get"))
            .thenReturn(httpClientActionDefinition);
        stubStoredType(workflowNodeTestOutput, "get");
        when(workflowNodeTestOutput.getOutput(any())).thenReturn(new OutputResponse(
            Property.toProperty(ComponentDsl.object("httpClient_1")
                .properties(ComponentDsl.string("author"))),
            null));
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput("wf-1", "httpClient_1", 3L))
            .thenReturn(Optional.of(workflowNodeTestOutput));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(HTTP_CLIENT_WORKFLOW, "wf-1", 3L);

        assertEquals(
            List.of("[logger_1] Property 'httpClient_1.quote' does not exist in the output of 'httpClient/v1/get'"),
            result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void aNodeWhoseComponentRequiresAConnectionButHasNoneIsAnError() {
        stubComponent("affinity", "Affinity", true);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(CONNECTION_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of("[affinity_1] Missing required connection: Affinity"), result.errors());
    }

    @Test
    void aNodeWithATestConnectionIsNotReported() {
        stubComponent("affinity", "Affinity", true);

        WorkflowTestConfigurationConnection connection = mock(WorkflowTestConfigurationConnection.class);

        when(connection.getWorkflowConnectionKey()).thenReturn("affinity");
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("wf-1", "affinity_1", 3L))
            .thenReturn(List.of(connection));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(CONNECTION_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of(), result.errors());
    }

    @Test
    void anOptionalConnectionIsNotReported() {
        stubComponent("affinity", "Affinity", false);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(CONNECTION_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of(), result.errors());
    }

    @Test
    void connectionsAreNotCheckedWithoutAWorkflowId() {
        stubComponent("affinity", "Affinity", true);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(CONNECTION_WORKFLOW, 3L);

        assertEquals(List.of(), result.errors());
    }

    @Test
    void aClusterElementMissingItsConnectionIsReportedUnderTheElementName() {
        stubComponent("aiAgent", "AI Agent", false);
        stubComponent("openAi", "OpenAI", true);
        stubComponent("httpClient", "HTTP Client", false);

        WorkflowTestConfigurationConnection toolConnection = mock(WorkflowTestConfigurationConnection.class);

        when(toolConnection.getWorkflowConnectionKey()).thenReturn("httpClient_1");
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("wf-1", "aiAgent_1", 3L))
            .thenReturn(List.of(toolConnection));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(AGENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of("[openAi_1] Missing required connection: OpenAI"), result.errors());
    }

    @Test
    void aNodeInsideABranchCaseMissingItsConnectionIsReported() {
        stubComponent("affinity", "Affinity", true);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("branch", 1))
            .thenReturn(new TaskDispatcherDefinition("branch"));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(BRANCH_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of("[affinity_1] Missing required connection: Affinity"), result.errors());
    }

    @Test
    void aClusterElementConnectionDoesNotSatisfyTheRootNode() {
        stubComponent("aiAgent", "AI Agent", true);
        stubComponent("openAi", "OpenAI", true);
        stubComponent("httpClient", "HTTP Client", false);

        WorkflowTestConfigurationConnection modelConnection = mock(WorkflowTestConfigurationConnection.class);

        when(modelConnection.getWorkflowConnectionKey()).thenReturn("openAi_1");
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("wf-1", "aiAgent_1", 3L))
            .thenReturn(List.of(modelConnection));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(AGENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of("[aiAgent_1] Missing required connection: AI Agent"), result.errors());
    }

    @Test
    void aRootConnectionKeyedByTheComponentNameSatisfiesTheRootNode() {
        stubComponent("aiAgent", "AI Agent", true);
        stubComponent("openAi", "OpenAI", true);
        stubComponent("httpClient", "HTTP Client", false);

        WorkflowTestConfigurationConnection rootConnection = mock(WorkflowTestConfigurationConnection.class);
        WorkflowTestConfigurationConnection modelConnection = mock(WorkflowTestConfigurationConnection.class);

        when(rootConnection.getWorkflowConnectionKey()).thenReturn("aiAgent");
        when(modelConnection.getWorkflowConnectionKey()).thenReturn("openAi_1");
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("wf-1", "aiAgent_1", 3L))
            .thenReturn(List.of(rootConnection, modelConnection));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(AGENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of(), result.errors());
    }

    @Test
    void aTestOutputRecordedForAnotherOperationIsIgnored() {
        ActionDefinition httpClientActionDefinition = actionDefinition(ComponentDsl.string("uri"));
        WorkflowNodeTestOutput workflowNodeTestOutput = mock(WorkflowNodeTestOutput.class);

        when(actionDefinitionService.getActionDefinition("httpClient", 1, "get"))
            .thenReturn(httpClientActionDefinition);
        stubStoredType(workflowNodeTestOutput, "post");
        when(workflowNodeTestOutput.getOutput(any())).thenReturn(new OutputResponse(
            Property.toProperty(ComponentDsl.object("httpClient_1")
                .properties(ComponentDsl.string("quote"))),
            null));
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput("wf-1", "httpClient_1", 3L))
            .thenReturn(Optional.of(workflowNodeTestOutput));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(HTTP_CLIENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(
            List.of("[logger_1] Property 'httpClient_1.quote' might not exist in the output of 'httpClient/v1/get'"),
            result.warnings());
    }

    private static void stubStoredType(WorkflowNodeTestOutput workflowNodeTestOutput, String operationName) {
        when(workflowNodeTestOutput.getTypeName()).thenReturn("httpClient");
        when(workflowNodeTestOutput.getTypeVersion()).thenReturn(1);
        when(workflowNodeTestOutput.getTypeOperationName()).thenReturn(operationName);
    }

    @Test
    void aNestedClusterElementMissingItsConnectionIsReported() {
        stubComponent("aiAgent", "AI Agent", false);
        stubComponent("openAi", "OpenAI", true);

        WorkflowTestConfigurationConnection outerModelConnection = mock(WorkflowTestConfigurationConnection.class);

        when(outerModelConnection.getWorkflowConnectionKey()).thenReturn("openAi_1");
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("wf-1", "aiAgent_1", 3L))
            .thenReturn(List.of(outerModelConnection));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(NESTED_AGENT_WORKFLOW, "wf-1", 3L);

        assertEquals(List.of("[openAi_2] Missing required connection: OpenAI"), result.errors());
    }

    private void stubComponent(String name, String title, boolean connectionRequired) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn(title);
        when(componentDefinition.isConnectionRequired()).thenReturn(connectionRequired);
        when(componentDefinitionService.getComponentDefinition(name, 1)).thenReturn(componentDefinition);
    }

    private static ActionDefinition actionDefinition(ComponentDsl.ModifiableStringProperty... properties) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        List<Property> propertyList = Arrays.stream(properties)
            .map(property -> (Property) Property.toProperty(property))
            .toList();

        doReturn(propertyList).when(actionDefinition)
            .getProperties();

        return actionDefinition;
    }
}
