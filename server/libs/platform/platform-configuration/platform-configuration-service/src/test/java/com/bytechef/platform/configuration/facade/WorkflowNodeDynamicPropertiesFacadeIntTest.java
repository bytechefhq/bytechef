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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.ComponentOperation;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WorkflowNodeDynamicPropertiesFacadeIntTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final ComponentOperation SHEETS_INSERT_ROW =
        new ComponentOperation("googleSheets", 1, "insertRow", false);

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ActionDefinitionService actionDefinitionService;

    @Autowired
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Autowired
    private WorkflowNodeDynamicPropertiesFacade workflowNodeDynamicPropertiesFacade;

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
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testGetClusterElementDynamicPropertiesResolvesConnectionByWorkflowConnectionKey() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "aiTask1",
                            "type": "aiAgent/v1/chat",
                            "parameters": {},
                            "clusterElements": {
                                "model": {
                                    "name": "openai1",
                                    "type": "openai/v1/chat",
                                    "parameters": {
                                        "model": "gpt-4"
                                    }
                                }
                            }
                        }
                    ]
                }
                """);

        String propertyName = "model";
        long expectedConnectionId = 100L;
        List<String> lookupDependsOnPaths = List.of("connectionId");

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "aiTask1", "openai1", expectedConnectionId, false, ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "aiTask1", "otherElement", 200L, false, ENVIRONMENT_ID);

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, "model"))
            .thenReturn(new ClusterElementType("model", "model", "Model"));

        List<Property> expectedProperties = List.of(mock(Property.class));
        Map<String, Long> expectedConnectionIds = Map.of("openai1", expectedConnectionId, "otherElement", 200L);

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), eq(Map.of("model", "gpt-4")), anyMap(),
            eq(lookupDependsOnPaths), eq(expectedConnectionId), eq(expectedConnectionIds), anyMap()))
                .thenReturn(expectedProperties);

        List<Property> properties = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
            workflow.getId(), "aiTask1", "model", "openai1", propertyName, lookupDependsOnPaths, ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        verify(clusterElementDefinitionFacade).executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), eq(Map.of("model", "gpt-4")), anyMap(),
            eq(lookupDependsOnPaths), eq(expectedConnectionId), eq(expectedConnectionIds), anyMap());
    }

    @Test
    void testGetClusterElementDynamicPropertiesReturnsNullConnectionWhenNoMatch() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "aiTask1",
                            "type": "aiAgent/v1/chat",
                            "parameters": {},
                            "clusterElements": {
                                "model": {
                                    "name": "openai1",
                                    "type": "openai/v1/chat",
                                    "parameters": {}
                                }
                            }
                        }
                    ]
                }
                """);

        String propertyName = "model";

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, "model"))
            .thenReturn(new ClusterElementType("model", "model", "Model"));

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), anyMap(),
            anyMap()))
                .thenReturn(expectedProperties);

        List<Property> properties = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
            workflow.getId(), "aiTask1", "model", "openai1", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        verify(clusterElementDefinitionFacade).executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(),
            eq(Map.of()), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetClusterElementDynamicPropertiesUsesWorkflowNodeNameForOutputLookup() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "previous_1",
                            "type": "httpClient/v1/get",
                            "parameters": {}
                        },
                        {
                            "name": "dataStream_1",
                            "type": "dataStream/v1/sync",
                            "parameters": {},
                            "clusterElements": {
                                "processor": {
                                    "name": "fieldMapper_1",
                                    "type": "fieldMapper/v1/map",
                                    "parameters": {
                                        "useJsonSchema": "=previous_1.value"
                                    }
                                }
                            }
                        }
                    ]
                }
                """);

        String propertyName = "useJsonSchema";

        saveSampleOutput(workflow.getId(), "previous_1", Map.of("value", "fromPrevious"));

        when(actionDefinitionService.getActionDefinition("httpClient", 1, "get"))
            .thenReturn(mock(ActionDefinition.class));
        when(clusterElementDefinitionService.getClusterElementType("dataStream", 1, "processor"))
            .thenReturn(new ClusterElementType("processor", "processor", "Processor"));

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("fieldMapper"), eq(1), eq("map"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), anyMap(),
            anyMap()))
                .thenReturn(expectedProperties);

        List<Property> properties = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
            workflow.getId(), "dataStream_1", "processor", "fieldMapper_1", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        ArgumentCaptor<Map<String, ?>> inputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionFacade).executeDynamicProperties(
            eq("fieldMapper"), eq(1), eq("map"), eq(propertyName), inputParametersArgumentCaptor.capture(), anyMap(),
            anyList(), isNull(), anyMap(), anyMap());

        Map<String, ?> inputParameters = inputParametersArgumentCaptor.getValue();

        assertThat(inputParameters.get("useJsonSchema")).isEqualTo("fromPrevious");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetClusterElementDynamicPropertiesEvaluatesParametersLeniently() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "aiAgent_1",
                            "type": "aiAgent/v1/chat",
                            "parameters": {},
                            "clusterElements": {
                                "tools": {
                                    "name": "dataTable_5",
                                    "type": "dataTable/v1/updateRecord",
                                    "parameters": {
                                        "broken": "=1 +",
                                        "values": {
                                            "handover_reason": "=fromAi()"
                                        }
                                    }
                                }
                            }
                        }
                    ]
                }
                """);

        String propertyName = "values";

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, "tools"))
            .thenReturn(new ClusterElementType("tools", "tools", "Tools"));

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("dataTable"), eq(1), eq("updateRecord"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(),
            anyMap(), anyMap()))
                .thenReturn(expectedProperties);

        List<Property> properties = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
            workflow.getId(), "aiAgent_1", "tools", "dataTable_5", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        ArgumentCaptor<Map<String, ?>> inputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionFacade).executeDynamicProperties(
            eq("dataTable"), eq(1), eq("updateRecord"), eq(propertyName), inputParametersArgumentCaptor.capture(),
            anyMap(), anyList(), isNull(), anyMap(), anyMap());

        Map<String, ?> inputParameters = inputParametersArgumentCaptor.getValue();

        assertThat(inputParameters.get("broken")).isEqualTo("=1 +");
        assertThat(inputParameters).containsKey("values");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetWorkflowNodeDynamicPropertiesForAction() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "httpClient1",
                            "type": "httpClient/v1/get",
                            "parameters": {
                                "body": "=fromAi()",
                                "broken": "=1 +"
                            }
                        }
                    ]
                }
                """);

        String propertyName = "bodyContentType";
        long connectionId = 50L;

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "httpClient1", "connection", connectionId, false, ENVIRONMENT_ID);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(actionDefinitionFacade.executeDynamicProperties(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), eq(workflow.getId()),
            eq(connectionId)))
                .thenReturn(expectedProperties);

        List<? extends BaseProperty> properties = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
            workflow.getId(), "httpClient1", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        ArgumentCaptor<Map<String, ?>> inputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionFacade).executeDynamicProperties(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), inputParametersArgumentCaptor.capture(), anyList(),
            eq(workflow.getId()), eq(connectionId));

        Map<String, ?> inputParameters = inputParametersArgumentCaptor.getValue();

        assertThat(inputParameters.get("broken")).isEqualTo("=1 +");
        assertThat(inputParameters).containsKey("body");
    }

    @Test
    void testGetWorkflowNodeDynamicPropertiesForTrigger() {
        Workflow workflow = createWorkflow(
            """
                {
                    "triggers": [
                        {
                            "name": "trigger1",
                            "type": "github/v1/newIssue",
                            "parameters": {}
                        }
                    ],
                    "tasks": []
                }
                """);

        String propertyName = "event";
        long connectionId = 75L;

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "trigger1", "connection", connectionId, true, ENVIRONMENT_ID);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(triggerDefinitionFacade.executeDynamicProperties(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId)))
                .thenReturn(expectedProperties);

        List<? extends BaseProperty> properties = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
            workflow.getId(), "trigger1", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        verify(triggerDefinitionFacade).executeDynamicProperties(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId));
    }

    @Test
    void testGetWorkflowNodeDynamicPropertiesForTaskDispatcher() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "subflow1",
                            "type": "subflow/v1",
                            "parameters": {}
                        }
                    ]
                }
                """);

        String propertyName = "inputs";

        when(taskDispatcherDefinitionService.executeDynamicProperties(eq("subflow"), eq(1), eq(propertyName), anyMap()))
            .thenReturn(List.of());

        List<? extends BaseProperty> properties = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
            workflow.getId(), "subflow1", propertyName, List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEmpty();

        verify(taskDispatcherDefinitionService).executeDynamicProperties(
            eq("subflow"), eq(1), eq(propertyName), anyMap());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesResolvesDeclaredConnectionByKey() {
        Workflow workflow = createScriptWorkflow();

        Map<String, Object> parameters = Map.of("spreadsheetId", "sheet1");
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(
                new ComponentConnection("googleSheets", 1, "script_1", "sheets", true),
                new ComponentConnection("googleSheets", 1, "script_1", "otherSheets", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "script_1", "otherSheets", 300L, false, ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "script_1", "sheets", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", parameters, List.of("spreadsheetId"), workflow.getId(), 100L))
                .thenReturn(expectedProperties);

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", parameters,
                List.of("spreadsheetId"), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesUsesSingleConnectionOfComponentWithoutKey() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "googleSheets_1",
                            "type": "googleSheets/v1/insertRow",
                            "parameters": {}
                        }
                    ]
                }
                """);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "googleSheets_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "googleSheets_1", "googleSheets", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "googleSheets_1", "googleSheets", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", Map.of(), List.of(), workflow.getId(), 100L))
                .thenReturn(expectedProperties);

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "googleSheets_1", null, SHEETS_INSERT_ROW, "values", null, Map.of(), List.of(),
                ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRunsTriggerDynamicProperties() {
        Workflow workflow = createWorkflow(
            """
                {
                    "triggers": [
                        {
                            "name": "trigger_1",
                            "type": "googleSheets/v1/newRow",
                            "parameters": {}
                        }
                    ],
                    "tasks": []
                }
                """);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "trigger_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "trigger_1", "googleSheets", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "trigger_1", "googleSheets", 100L, true, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(triggerDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "newRow", "columns", Map.of(), List.of(), 100L))
                .thenReturn(expectedProperties);

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "trigger_1", null, new ComponentOperation("googleSheets", 1, "newRow", true),
                "columns", null, Map.of(), List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesResolvesClusterElementConnectionUnderRootNode() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "aiAgent_1",
                            "type": "aiAgent/v1/chat",
                            "parameters": {},
                            "clusterElements": {
                                "tools": {
                                    "name": "script_1",
                                    "type": "script/v1/javascript",
                                    "parameters": {}
                                }
                            }
                        }
                    ]
                }
                """);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getClusterElementComponentConnections(
            workflow.getId(), "aiAgent_1", "tools", "script_1"))
                .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "script_1", "sheets", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "aiAgent_1", "sheets", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", Map.of(), List.of(), workflow.getId(), 100L))
                .thenReturn(expectedProperties);

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "aiAgent_1", new ClusterElementNode("tools", "script_1"), SHEETS_INSERT_ROW,
                "values", "sheets", Map.of(), List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);

        verify(componentConnectionFacade, never()).getWorkflowNodeComponentConnections(anyString(), anyString());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesReturnsEmptyWhenDeclaredConnectionIsNotConfigured() {
        Workflow workflow = createScriptWorkflow();

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "script_1", "sheets", true)));

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", Map.of(), List.of(),
                ENVIRONMENT_ID);

        assertThat(properties).isEmpty();

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRejectsKeyDeclaredForAnotherComponent() {
        Workflow workflow = createScriptWorkflow();

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "script_1", "sheets", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "script_1", "sheets", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", Map.of(), List.of(),
                ENVIRONMENT_ID);

        assertThat(properties).isEmpty();

        verify(connectionService, never()).fetchConnection(anyLong());
        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesReturnsEmptyWhenConnectionIsAmbiguous() {
        Workflow workflow = createScriptWorkflow();

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(
                new ComponentConnection("googleSheets", 1, "script_1", "sheets", true),
                new ComponentConnection("googleSheets", 1, "script_1", "otherSheets", true)));

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "script_1", null, SHEETS_INSERT_ROW, "values", null, Map.of(), List.of(),
                ENVIRONMENT_ID);

        assertThat(properties).isEmpty();

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRunsWithoutConnectionWhenNoneIsDeclared() {
        Workflow workflow = createScriptWorkflow();

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of());
        when(actionDefinitionFacade.executeDynamicProperties(
            "jsonHelper", 1, "parse", "schema", Map.of(), List.of(), workflow.getId(), null))
                .thenReturn(expectedProperties);

        List<Property> properties =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                workflow.getId(), "script_1", null, new ComponentOperation("jsonHelper", 1, "parse", false), "schema",
                null, Map.of(), List.of(), ENVIRONMENT_ID);

        assertThat(properties).isEqualTo(expectedProperties);
    }

    private static Connection connectionOf(String componentName) {
        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn(componentName);

        return connection;
    }

    private Workflow createScriptWorkflow() {
        return createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "script_1",
                            "type": "script/v1/javascript",
                            "parameters": {}
                        }
                    ]
                }
                """);
    }

    private Workflow createWorkflow(String definition) {
        return workflowService.create(definition, Format.JSON, Workflow.SourceType.JDBC);
    }

    private void saveSampleOutput(String workflowId, String workflowNodeName, Map<String, ?> sampleOutput) {
        workflowNodeTestOutputService.save(
            workflowId, workflowNodeName, WorkflowNodeType.ofType("httpClient/v1/get"),
            new OutputResponse(new ObjectProperty(ComponentDsl.object()), sampleOutput, null), false,
            ENVIRONMENT_ID);
    }
}
