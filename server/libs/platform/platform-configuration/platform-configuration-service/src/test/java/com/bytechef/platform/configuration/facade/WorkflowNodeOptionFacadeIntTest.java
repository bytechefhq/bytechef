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
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.ComponentOperation;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
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
class WorkflowNodeOptionFacadeIntTest {

    private static final long ENVIRONMENT_ID = 1L;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

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
    private WorkflowNodeOptionFacade workflowNodeOptionFacade;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @AfterEach
    void afterEach() {
        workflowTestConfigurationRepository.deleteAll();
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testGetWorkflowNodeOptionsForTaskDispatcher() {
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

        String propertyName = "workflowUuid";
        String searchText = "invoice";

        com.bytechef.platform.workflow.task.dispatcher.domain.Option taskDispatcherOption =
            mock(com.bytechef.platform.workflow.task.dispatcher.domain.Option.class);

        when(taskDispatcherDefinitionService.executeOptions("subflow", 1, propertyName, searchText))
            .thenReturn(List.of(taskDispatcherOption));

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeOptions(
            workflow.getId(), "subflow1", propertyName, List.of(), searchText, ENVIRONMENT_ID);

        assertThat(options).hasSize(1);

        verify(taskDispatcherDefinitionService).executeOptions("subflow", 1, propertyName, searchText);
    }

    @Test
    void testGetWorkflowNodeOptionsForAction() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "httpClient1",
                            "type": "httpClient/v1/get",
                            "parameters": {}
                        }
                    ]
                }
                """);

        String propertyName = "method";
        List<Option> expectedOptions = List.of(mock(Option.class));

        when(actionDefinitionFacade.executeOptions(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(), anyMap(), anyMap()))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeOptions(
            workflow.getId(), "httpClient1", propertyName, List.of(), null, ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);

        verify(actionDefinitionFacade).executeOptions(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(), anyMap(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetWorkflowNodeOptionsForActionFiltersConnectionsByNodeName() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "agent1",
                            "type": "httpClient/v1/get",
                            "parameters": {}
                        },
                        {
                            "name": "agent2",
                            "type": "httpClient/v1/get",
                            "parameters": {}
                        }
                    ]
                }
                """);

        String sharedConnectionKey = "jdbc";
        String propertyName = "model";
        long agent1ConnectionId = 100L;
        long agent2ConnectionId = 200L;

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "agent2", sharedConnectionKey, agent2ConnectionId, false, ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "agent2", "openAi", 300L, false, ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "agent1", sharedConnectionKey, agent1ConnectionId, false, ENVIRONMENT_ID);

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(actionDefinitionFacade.executeOptions(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(), anyMap(), anyMap()))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeOptions(
            workflow.getId(), "agent1", propertyName, List.of(), null, ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);

        ArgumentCaptor<Map<String, Long>> connectionIdsArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionFacade).executeOptions(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(),
            connectionIdsArgumentCaptor.capture(), anyMap());

        assertThat(connectionIdsArgumentCaptor.getValue()).containsExactly(
            Map.entry(sharedConnectionKey, agent1ConnectionId));
    }

    @Test
    void testGetWorkflowNodeOptionsForTrigger() {
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

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(triggerDefinitionFacade.executeOptions(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId)))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeOptions(
            workflow.getId(), "trigger1", propertyName, List.of(), null, ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);

        verify(triggerDefinitionFacade).executeOptions(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetClusterElementNodeOptionsAggregatesNestedClusterElementParameters() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "aiAgent_1",
                            "type": "aiAgent/v1",
                            "parameters": {},
                            "clusterElements": {
                                "tools": [
                                    {
                                        "name": "aiAgent_tool_1",
                                        "type": "aiAgent/v1/tool",
                                        "label": "AI Agent Tool",
                                        "parameters": {
                                            "prompt": "do something"
                                        },
                                        "clusterElements": {
                                            "tools": [
                                                {
                                                    "name": "hubspot_1",
                                                    "type": "hubspot/v1/createContact",
                                                    "label": "Hubspot",
                                                    "parameters": {
                                                        "apiKey": "secret"
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
                """);

        String clusterElementTypeName = "tools";
        String propertyName = "objectType";

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, clusterElementTypeName))
            .thenReturn(new ClusterElementType(clusterElementTypeName, "tools", "Tools", true, false));

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(clusterElementDefinitionFacade.executeOptions(
            eq("aiAgent"), eq(1), eq("tool"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), isNull(),
            anyMap(), anyMap()))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getClusterElementNodeOptions(
            workflow.getId(), "aiAgent_1", clusterElementTypeName, "aiAgent_tool_1", propertyName, List.of(), null,
            ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);

        ArgumentCaptor<Map<String, Map<String, ?>>> inputParametersArgumentCaptor =
            ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionFacade).executeOptions(
            eq("aiAgent"), eq(1), eq("tool"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), isNull(),
            anyMap(), inputParametersArgumentCaptor.capture());

        Map<String, Map<String, ?>> inputParameters = inputParametersArgumentCaptor.getValue();

        assertThat(inputParameters).containsKeys("aiAgent_tool_1", "hubspot_1");
        Map<String, ?> hubspotParameters = inputParameters.get("hubspot_1");

        assertThat(hubspotParameters.get("apiKey")).isEqualTo("secret");
    }

    @Test
    void testGetWorkflowNodeComponentOperationOptionsResolvesDeclaredConnection() {
        Workflow workflow = createWorkflow(
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

        Map<String, Object> parameters = Map.of("baseId", "app1");
        List<Option> expectedOptions = List.of(mock(Option.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(new ComponentConnection("airtable", 1, "script_1", "at", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "script_1", "at", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("airtable");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeOptions(
            "airtable", 1, "createRecord", "tableId", parameters, List.of("baseId"), null, 100L))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeComponentOperationOptions(
            workflow.getId(), "script_1", null, new ComponentOperation("airtable", 1, "createRecord", false),
            "tableId", "at", parameters, List.of("baseId"), ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);
    }

    @Test
    void testGetWorkflowNodeComponentOperationOptionsRunsTriggerOptionsForClusterElement() {
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

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(componentConnectionFacade.getClusterElementComponentConnections(
            workflow.getId(), "aiAgent_1", "tools", "script_1"))
                .thenReturn(List.of(new ComponentConnection("airtable", 1, "script_1", "at", true)));

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "aiAgent_1", "at", 100L, false, ENVIRONMENT_ID);

        Connection boundConnection = connectionOf("airtable");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(triggerDefinitionFacade.executeOptions(
            "airtable", 1, "newRecord", "tableId", Map.of(), List.of(), null, 100L))
                .thenReturn(expectedOptions);

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeComponentOperationOptions(
            workflow.getId(), "aiAgent_1", new ClusterElementNode("tools", "script_1"),
            new ComponentOperation("airtable", 1, "newRecord", true), "tableId", null, Map.of(), List.of(),
            ENVIRONMENT_ID);

        assertThat(options).isEqualTo(expectedOptions);
    }

    @Test
    void testGetWorkflowNodeComponentOperationOptionsReturnsEmptyWithoutTestConnection() {
        Workflow workflow = createWorkflow(
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

        when(componentConnectionFacade.getWorkflowNodeComponentConnections(workflow.getId(), "script_1"))
            .thenReturn(List.of(new ComponentConnection("airtable", 1, "script_1", "at", true)));

        List<Option> options = workflowNodeOptionFacade.getWorkflowNodeComponentOperationOptions(
            workflow.getId(), "script_1", null, new ComponentOperation("airtable", 1, "createRecord", false),
            "tableId", "at", Map.of(), List.of(), ENVIRONMENT_ID);

        assertThat(options).isEmpty();

        verify(actionDefinitionFacade, never()).executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), any(), any(Long.class));
    }

    private static Connection connectionOf(String componentName) {
        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn(componentName);

        return connection;
    }

    private Workflow createWorkflow(String definition) {
        return workflowService.create(definition, Format.JSON, Workflow.SourceType.JDBC);
    }
}
