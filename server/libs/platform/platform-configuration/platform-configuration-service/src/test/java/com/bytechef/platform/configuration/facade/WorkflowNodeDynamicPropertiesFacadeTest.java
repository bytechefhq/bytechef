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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.ComponentOperation;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowNodeDynamicPropertiesFacadeTest {

    private static final ComponentOperation SHEETS_INSERT_ROW =
        new ComponentOperation("googleSheets", 1, "insertRow", false);

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeDynamicPropertiesFacadeImpl workflowNodeDynamicPropertiesFacade;

    @BeforeEach
    void setUp() {
        workflowNodeDynamicPropertiesFacade = new WorkflowNodeDynamicPropertiesFacadeImpl(
            actionDefinitionFacade, clusterElementDefinitionFacade, clusterElementDefinitionService,
            new ComponentOperationConnectionResolver(
                componentConnectionFacade, connectionService, workflowTestConfigurationService),
            evaluator,
            taskDispatcherDefinitionService, triggerDefinitionFacade, workflowService, workflowNodeOutputFacade,
            workflowTestConfigurationService);
    }

    @Test
    void testGetClusterElementDynamicPropertiesResolvesConnectionByWorkflowConnectionKey() {
        String workflowId = "workflow1";
        String workflowNodeName = "aiTask1";
        String clusterElementTypeName = "model";
        String clusterElementWorkflowNodeName = "openai1";
        String propertyName = "model";
        long environmentId = 1L;
        long expectedConnectionId = 100L;

        WorkflowTestConfigurationConnection matchingConnection =
            new WorkflowTestConfigurationConnection(expectedConnectionId, clusterElementWorkflowNodeName, "aiTask1");
        WorkflowTestConfigurationConnection otherConnection =
            new WorkflowTestConfigurationConnection(200L, "otherElement", "aiTask1");

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            environmentId, Map.of(), workflowId, List.of(matchingConnection, otherConnection));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId))
            .thenReturn(Optional.of(workflowTestConfiguration));
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("aiAgent/v1/chat");
        when(workflowTask.getName()).thenReturn(workflowNodeName);

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        ClusterElementType clusterElementType = new ClusterElementType("model", "model", "Model");

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, clusterElementTypeName))
            .thenReturn(clusterElementType);

        ClusterElement clusterElement = mock(ClusterElement.class);

        Map<String, Object> clusterElementParameters = Map.of("model", "gpt-4");

        when(clusterElement.getType()).thenReturn("openai/v1/chat");
        doReturn(clusterElementParameters).when(clusterElement)
            .getParameters();

        ClusterElementMap clusterElementMap = mock(ClusterElementMap.class);

        when(clusterElementMap.getClusterElement(clusterElementType, clusterElementWorkflowNodeName))
            .thenReturn(clusterElement);
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        Map<String, ClusterElement> model = Map.of("model", clusterElement);

        doReturn(model.entrySet()).when(clusterElementMap)
            .entrySet();

        List<Property> expectedProperties = List.of(mock(Property.class));
        List<String> lookupDependsOnPaths = List.of("connectionId");
        Map<String, Long> expectedConnectionIds = Map.of(
            clusterElementWorkflowNodeName, expectedConnectionId, "otherElement", 200L);

        doReturn(clusterElementParameters).when(evaluator)
            .evaluate(anyMap(), anyMap(), anyBoolean());
        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), eq(lookupDependsOnPaths),
            eq(expectedConnectionId), eq(expectedConnectionIds), anyMap()))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, lookupDependsOnPaths, environmentId);

            assertEquals(expectedProperties, result);

            verify(clusterElementDefinitionFacade).executeDynamicProperties(
                eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), eq(lookupDependsOnPaths),
                eq(expectedConnectionId), eq(expectedConnectionIds), anyMap());

            verify(workflowNodeOutputFacade).getPreviousWorkflowNodeSampleOutputs(
                workflowId, workflowNodeName, environmentId);
        }
    }

    @Test
    void testGetClusterElementDynamicPropertiesReturnsNullConnectionWhenNoMatch() {
        String workflowId = "workflow1";
        String workflowNodeName = "aiTask1";
        String clusterElementTypeName = "model";
        String clusterElementWorkflowNodeName = "openai1";
        String propertyName = "model";
        long environmentId = 1L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId))
            .thenReturn(Optional.empty());
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("aiAgent/v1/chat");
        when(workflowTask.getName()).thenReturn(workflowNodeName);

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        ClusterElementType clusterElementType = new ClusterElementType("model", "model", "Model");

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, clusterElementTypeName))
            .thenReturn(clusterElementType);

        ClusterElement clusterElement = mock(ClusterElement.class);

        when(clusterElement.getType()).thenReturn("openai/v1/chat");
        doReturn(Map.of()).when(clusterElement)
            .getParameters();

        ClusterElementMap clusterElementMap = mock(ClusterElementMap.class);

        when(clusterElementMap.getClusterElement(clusterElementType, clusterElementWorkflowNodeName))
            .thenReturn(clusterElement);
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();
        doReturn(Map.of("model", clusterElement)
            .entrySet()).when(clusterElementMap)
                .entrySet();

        List<Property> expectedProperties = List.of(mock(Property.class));

        doReturn(Map.of()).when(evaluator)
            .evaluate(anyMap(), anyMap(), anyBoolean());
        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), anyMap(),
            anyMap()))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(clusterElementDefinitionFacade).executeDynamicProperties(
                eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), anyMap(),
                anyMap());
        }
    }

    @Test
    void testGetClusterElementDynamicPropertiesUsesWorkflowNodeNameForOutputLookup() {
        String workflowId = "workflow1";
        String workflowNodeName = "dataStream_1";
        String clusterElementTypeName = "processor";
        String clusterElementWorkflowNodeName = "fieldMapper_1";
        String propertyName = "useJsonSchema";
        long environmentId = 1L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId))
            .thenReturn(Optional.empty());
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("dataStream/v1/sync");
        when(workflowTask.getName()).thenReturn(workflowNodeName);

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        ClusterElementType clusterElementType = new ClusterElementType("processor", "processor", "Processor");

        when(clusterElementDefinitionService.getClusterElementType("dataStream", 1, clusterElementTypeName))
            .thenReturn(clusterElementType);

        ClusterElement clusterElement = mock(ClusterElement.class);

        when(clusterElement.getType()).thenReturn("fieldMapper/v1/map");
        doReturn(Map.of()).when(clusterElement)
            .getParameters();

        ClusterElementMap clusterElementMap = mock(ClusterElementMap.class);

        when(clusterElementMap.getClusterElement(clusterElementType, clusterElementWorkflowNodeName))
            .thenReturn(clusterElement);
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        Map<String, ClusterElement> processor = Map.of("processor", clusterElement);

        doReturn(processor.entrySet()).when(clusterElementMap)
            .entrySet();

        List<Property> expectedProperties = List.of(mock(Property.class));

        doReturn(Map.of()).when(evaluator)
            .evaluate(anyMap(), anyMap(), anyBoolean());
        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("fieldMapper"), eq(1), eq("map"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), anyMap(),
            anyMap()))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(workflowNodeOutputFacade).getPreviousWorkflowNodeSampleOutputs(
                workflowId, workflowNodeName, environmentId);
        }
    }

    @Test
    void testGetClusterElementDynamicPropertiesEvaluatesParametersLeniently() {
        String workflowId = "workflow1";
        String workflowNodeName = "aiAgent_1";
        String clusterElementTypeName = "tools";
        String clusterElementWorkflowNodeName = "dataTable_5";
        String propertyName = "values";
        long environmentId = 1L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId))
            .thenReturn(Optional.empty());
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("aiAgent/v1/chat");
        when(workflowTask.getName()).thenReturn(workflowNodeName);

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        ClusterElementType clusterElementType = new ClusterElementType("tools", "tools", "Tools");

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, clusterElementTypeName))
            .thenReturn(clusterElementType);

        ClusterElement clusterElement = mock(ClusterElement.class);

        Map<String, Object> clusterElementParameters = Map.of("values", Map.of("handover_reason", "=fromAi()"));

        when(clusterElement.getType()).thenReturn("dataTable/v1/updateRecord");
        doReturn(clusterElementParameters).when(clusterElement)
            .getParameters();

        ClusterElementMap clusterElementMap = mock(ClusterElementMap.class);

        when(clusterElementMap.getClusterElement(clusterElementType, clusterElementWorkflowNodeName))
            .thenReturn(clusterElement);
        doReturn(Map.of()).when(workflowTask)
            .getExtensions();

        Map<String, ClusterElement> tools = Map.of("tools", clusterElement);

        doReturn(tools.entrySet()).when(clusterElementMap)
            .entrySet();

        List<Property> expectedProperties = List.of(mock(Property.class));

        doReturn(clusterElementParameters).when(evaluator)
            .evaluate(anyMap(), anyMap(), eq(true));

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("dataTable"), eq(1), eq("updateRecord"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(),
            anyMap(), anyMap()))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(evaluator, never()).evaluate(anyMap(), anyMap());
        }
    }

    @Test
    void testGetWorkflowNodeDynamicPropertiesForAction() {
        String workflowId = "workflow1";
        String workflowNodeName = "httpClient1";
        String propertyName = "bodyContentType";
        long environmentId = 1L;
        long connectionId = 50L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            workflowId, workflowNodeName, environmentId))
                .thenReturn(Optional.of(connectionId));
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("httpClient/v1/get");
        when(workflowTask.getName()).thenReturn(workflowNodeName);
        doReturn(Map.of()).when(workflowTask)
            .evaluateParameters(anyMap(), any(Evaluator.class), anyBoolean());

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(actionDefinitionFacade.executeDynamicProperties(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), eq(workflowId),
            eq(connectionId)))
                .thenReturn(expectedProperties);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.empty());

            List<? extends BaseProperty> result = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                workflowId, workflowNodeName, propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(actionDefinitionFacade).executeDynamicProperties(
                eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), eq(workflowId),
                eq(connectionId));

            verify(workflowTask).evaluateParameters(anyMap(), any(Evaluator.class), eq(true));
        }
    }

    @Test
    void testGetWorkflowNodeDynamicPropertiesForTrigger() {
        String workflowId = "workflow1";
        String workflowNodeName = "trigger1";
        String propertyName = "event";
        long environmentId = 1L;
        long connectionId = 75L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            workflowId, workflowNodeName, environmentId))
                .thenReturn(Optional.of(connectionId));
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflowTrigger.getType()).thenReturn("github/v1/newIssue");
        doReturn(Map.of()).when(workflowTrigger)
            .evaluateParameters(anyMap(), any(Evaluator.class), anyBoolean());

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(triggerDefinitionFacade.executeDynamicProperties(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId)))
                .thenReturn(expectedProperties);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.of(workflowTrigger));

            List<? extends BaseProperty> result = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                workflowId, workflowNodeName, propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(triggerDefinitionFacade).executeDynamicProperties(
                eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId));
        }
    }

    @Test
    void testGetWorkflowNodeDynamicPropertiesForTaskDispatcher() {
        String workflowId = "workflow1";
        String workflowNodeName = "subflow1";
        String propertyName = "inputs";
        long environmentId = 1L;

        when(workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            workflowId, workflowNodeName, environmentId))
                .thenReturn(Optional.empty());
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("subflow/v1");
        doReturn(Map.of()).when(workflowTask)
            .evaluateParameters(anyMap(), any(Evaluator.class), anyBoolean());

        List<com.bytechef.platform.workflow.task.dispatcher.domain.Property> expectedProperties = List.of();

        when(taskDispatcherDefinitionService.executeDynamicProperties(
            eq("subflow"), eq(1), eq(propertyName), anyMap()))
                .thenReturn(expectedProperties);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.empty());

            List<? extends BaseProperty> result = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                workflowId, workflowNodeName, propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(taskDispatcherDefinitionService).executeDynamicProperties(
                eq("subflow"), eq(1), eq(propertyName), anyMap());
        }
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesResolvesDeclaredConnectionByKey() {
        Map<String, Object> parameters = Map.of("spreadsheetId", "sheet1");
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(
                new ComponentConnection("googleSheets", 1, "script_1", "sheets", true),
                new ComponentConnection("googleSheets", 1, "script_1", "otherSheets", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "script_1", 1L))
            .thenReturn(List.of(
                new WorkflowTestConfigurationConnection(300L, "otherSheets", "script_1"),
                new WorkflowTestConfigurationConnection(100L, "sheets", "script_1")));
        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", parameters, List.of("spreadsheetId"), "workflow1", 100L))
                .thenReturn(expectedProperties);

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", parameters,
                List.of("spreadsheetId"), 1L);

        assertEquals(expectedProperties, result);
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesUsesSingleConnectionOfComponentWithoutKey() {
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "googleSheets_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "googleSheets_1", "googleSheets", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            "workflow1", "googleSheets_1", 1L))
                .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "googleSheets", "googleSheets_1")));
        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", Map.of(), List.of(), "workflow1", 100L))
                .thenReturn(expectedProperties);

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "googleSheets_1", null, SHEETS_INSERT_ROW, "values", null, Map.of(), List.of(), 1L);

        assertEquals(expectedProperties, result);
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRunsTriggerDynamicProperties() {
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "trigger_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "trigger_1", "googleSheets", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "trigger_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "googleSheets", "trigger_1")));
        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(triggerDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "newRow", "columns", Map.of(), List.of(), 100L))
                .thenReturn(expectedProperties);

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "trigger_1", null, new ComponentOperation("googleSheets", 1, "newRow", true), "columns",
                null, Map.of(), List.of(), 1L);

        assertEquals(expectedProperties, result);

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesResolvesClusterElementConnectionUnderRootNode() {
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getClusterElementComponentConnections(
            "workflow1", "aiAgent_1", "tools", "script_1"))
                .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "script_1", "sheets", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "aiAgent_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "sheets", "aiAgent_1")));
        Connection boundConnection = connectionOf("googleSheets");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));
        when(actionDefinitionFacade.executeDynamicProperties(
            "googleSheets", 1, "insertRow", "values", Map.of(), List.of(), "workflow1", 100L))
                .thenReturn(expectedProperties);

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "aiAgent_1", new ClusterElementNode("tools", "script_1"), SHEETS_INSERT_ROW, "values",
                "sheets", Map.of(), List.of(), 1L);

        assertEquals(expectedProperties, result);

        verify(componentConnectionFacade, never()).getWorkflowNodeComponentConnections(anyString(), anyString());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesReturnsEmptyWhenDeclaredConnectionIsNotConfigured() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(new ComponentConnection("googleSheets", 1, "script_1", "sheets", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "script_1", 1L))
            .thenReturn(List.of());

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", Map.of(), List.of(), 1L);

        assertTrue(result.isEmpty());

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRejectsKeyDeclaredForAnotherComponent() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "script_1", "sheets", true)));

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "script_1", null, SHEETS_INSERT_ROW, "values", "sheets", Map.of(), List.of(), 1L);

        assertTrue(result.isEmpty());

        verify(workflowTestConfigurationService, never()).getWorkflowTestConfigurationConnections(
            anyString(), anyString(), eq(1L));
        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesReturnsEmptyWhenConnectionIsAmbiguous() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(
                new ComponentConnection("googleSheets", 1, "script_1", "sheets", true),
                new ComponentConnection("googleSheets", 1, "script_1", "otherSheets", true)));

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "script_1", null, SHEETS_INSERT_ROW, "values", null, Map.of(), List.of(), 1L);

        assertTrue(result.isEmpty());

        verify(actionDefinitionFacade, never()).executeDynamicProperties(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyList(), anyString(), any());
    }

    @Test
    void testGetWorkflowNodeComponentOperationDynamicPropertiesRunsWithoutConnectionWhenNoneIsDeclared() {
        List<Property> expectedProperties = List.of(mock(Property.class));

        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of());
        when(actionDefinitionFacade.executeDynamicProperties(
            "jsonHelper", 1, "parse", "schema", Map.of(), List.of(), "workflow1", null))
                .thenReturn(expectedProperties);

        List<Property> result =
            workflowNodeDynamicPropertiesFacade.getWorkflowNodeComponentOperationDynamicProperties(
                "workflow1", "script_1", null, new ComponentOperation("jsonHelper", 1, "parse", false), "schema",
                null, Map.of(), List.of(), 1L);

        assertEquals(expectedProperties, result);
    }

    private static Connection connectionOf(String componentName) {
        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn(componentName);

        return connection;
    }
}
