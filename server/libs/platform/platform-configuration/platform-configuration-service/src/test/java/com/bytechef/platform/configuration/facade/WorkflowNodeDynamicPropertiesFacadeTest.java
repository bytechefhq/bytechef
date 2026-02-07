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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
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

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private Evaluator evaluator;

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
            actionDefinitionFacade, clusterElementDefinitionFacade, clusterElementDefinitionService, evaluator,
            triggerDefinitionFacade, workflowService, workflowNodeOutputFacade, workflowTestConfigurationService);
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

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq("aiAgent"), eq(environmentId));

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

        List<Property> expectedProperties = List.of(mock(Property.class));

        doReturn(clusterElementParameters).when(evaluator)
            .evaluate(anyMap(), anyMap());
        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyList(), eq(expectedConnectionId)))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(clusterElementDefinitionFacade).executeDynamicProperties(
                eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyList(), eq(expectedConnectionId));
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

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq("aiAgent"), eq(environmentId));

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

        List<Property> expectedProperties = List.of(mock(Property.class));

        doReturn(Map.of()).when(evaluator)
            .evaluate(anyMap(), anyMap());
        when(clusterElementDefinitionFacade.executeDynamicProperties(
            eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyList(), isNull()))
                .thenReturn(expectedProperties);

        try (MockedStatic<ClusterElementMap> mockedClusterElementMap = mockStatic(ClusterElementMap.class)) {
            mockedClusterElementMap.when(() -> ClusterElementMap.of(anyMap()))
                .thenReturn(clusterElementMap);

            List<Property> result = workflowNodeDynamicPropertiesFacade.getClusterElementDynamicProperties(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(clusterElementDefinitionFacade).executeDynamicProperties(
                eq("openai"), eq(1), eq("chat"), eq(propertyName), anyMap(), anyList(), isNull());
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
            .evaluateParameters(anyMap(), any(Evaluator.class));

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

            List<Property> result = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                workflowId, workflowNodeName, propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(actionDefinitionFacade).executeDynamicProperties(
                eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), eq(workflowId),
                eq(connectionId));
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
            .evaluateParameters(anyMap(), any(Evaluator.class));

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(triggerDefinitionFacade.executeDynamicProperties(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId)))
                .thenReturn(expectedProperties);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.of(workflowTrigger));

            List<Property> result = workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                workflowId, workflowNodeName, propertyName, List.of(), environmentId);

            assertEquals(expectedProperties, result);

            verify(triggerDefinitionFacade).executeDynamicProperties(
                eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), eq(connectionId));
        }
    }
}
