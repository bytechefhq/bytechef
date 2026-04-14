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
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeOptionFacadeTest {

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

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

    private WorkflowNodeOptionFacadeImpl workflowNodeOptionFacade;

    @BeforeEach
    void setUp() {
        workflowNodeOptionFacade = new WorkflowNodeOptionFacadeImpl(
            evaluator, actionDefinitionFacade, clusterElementDefinitionFacade, clusterElementDefinitionService,
            taskDispatcherDefinitionService, triggerDefinitionFacade, workflowService, workflowNodeOutputFacade,
            workflowTestConfigurationService);
    }

    @Test
    void testGetWorkflowNodeOptionsForTaskDispatcher() {
        String workflowId = "workflow1";
        String workflowNodeName = "subflow1";
        String propertyName = "workflowUuid";
        String searchText = "invoice";
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

        com.bytechef.platform.workflow.task.dispatcher.domain.Option taskDispatcherOption =
            mock(com.bytechef.platform.workflow.task.dispatcher.domain.Option.class);

        when(taskDispatcherDefinitionService.executeOptions(
            eq("subflow"), eq(1), eq(propertyName), eq(searchText)))
                .thenReturn(List.of(taskDispatcherOption));

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.empty());

            List<Option> result = workflowNodeOptionFacade.getWorkflowNodeOptions(
                workflowId, workflowNodeName, propertyName, List.of(), searchText, environmentId);

            assertEquals(1, result.size());

            verify(taskDispatcherDefinitionService).executeOptions(
                eq("subflow"), eq(1), eq(propertyName), eq(searchText));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetWorkflowNodeOptionsForAction() {
        String workflowId = "workflow1";
        String workflowNodeName = "httpClient1";
        String propertyName = "method";
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

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(actionDefinitionFacade.executeOptions(
            eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId)))
                .thenReturn(expectedOptions);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.empty());

            List<Option> result = workflowNodeOptionFacade.getWorkflowNodeOptions(
                workflowId, workflowNodeName, propertyName, List.of(), null, environmentId);

            assertEquals(expectedOptions, result);

            verify(actionDefinitionFacade).executeOptions(
                eq("httpClient"), eq(1), eq("get"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId));
        }
    }

    @Test
    void testGetWorkflowNodeOptionsForTrigger() {
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

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(triggerDefinitionFacade.executeOptions(
            eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId)))
                .thenReturn(expectedOptions);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.fetch(workflow, workflowNodeName))
                .thenReturn(Optional.of(workflowTrigger));

            List<Option> result = workflowNodeOptionFacade.getWorkflowNodeOptions(
                workflowId, workflowNodeName, propertyName, List.of(), null, environmentId);

            assertEquals(expectedOptions, result);

            verify(triggerDefinitionFacade).executeOptions(
                eq("github"), eq(1), eq("newIssue"), eq(propertyName), anyMap(), anyList(), isNull(), eq(connectionId));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetClusterElementNodeOptionsAggregatesNestedClusterElementParameters() {
        String workflowId = "workflow1";
        String workflowNodeName = "aiAgent_1";
        String clusterElementTypeName = "tools";
        String clusterElementWorkflowNodeName = "aiAgent_tool_1";
        String propertyName = "objectType";
        long environmentId = 1L;

        Map<String, ?> nestedToolMap = Map.of(
            "name", "hubspot_1",
            "type", "hubspot/v1/createContact",
            "label", "Hubspot",
            "parameters", Map.of("apiKey", "secret"));

        Map<String, ?> outerToolMap = Map.of(
            "name", clusterElementWorkflowNodeName,
            "type", "aiAgent/v1/tool",
            "label", "AI Agent Tool",
            "parameters", Map.of("prompt", "do something"),
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("tools", List.of(nestedToolMap)));

        Map<String, ?> taskExtensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("tools", List.of(outerToolMap)));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId))
            .thenReturn(Optional.empty());
        doReturn(Map.of()).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs(workflowId, environmentId);

        Workflow workflow = mock(Workflow.class);
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);
        when(workflowTask.getType()).thenReturn("aiAgent/v1");
        when(workflowTask.getName()).thenReturn(workflowNodeName);
        doReturn(taskExtensions).when(workflowTask)
            .getExtensions();

        doReturn(Map.of()).when(workflowNodeOutputFacade)
            .getPreviousWorkflowNodeSampleOutputs(eq(workflowId), eq(workflowNodeName), eq(environmentId));

        ClusterElementType clusterElementType = new ClusterElementType(
            clusterElementTypeName, "tools", "Tools", true, false);

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, clusterElementTypeName))
            .thenReturn(clusterElementType);

        when(evaluator.evaluate(anyMap(), anyMap()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<Option> expectedOptions = List.of(mock(Option.class));

        when(clusterElementDefinitionFacade.executeOptions(
            eq("aiAgent"), eq(1), eq("tool"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), isNull(),
            anyMap(), anyMap()))
                .thenReturn(expectedOptions);

        List<Option> result = workflowNodeOptionFacade.getClusterElementNodeOptions(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, propertyName,
            List.of(), null, environmentId);

        assertEquals(expectedOptions, result);

        ArgumentCaptor<Map<String, Map<String, ?>>> captor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionFacade).executeOptions(
            eq("aiAgent"), eq(1), eq("tool"), eq(propertyName), anyMap(), anyMap(), anyList(), isNull(), isNull(),
            anyMap(), captor.capture());

        Map<String, Map<String, ?>> capturedInputParameters = captor.getValue();

        assertEquals(true, capturedInputParameters.containsKey("aiAgent_tool_1"));
        assertEquals(true, capturedInputParameters.containsKey("hubspot_1"));
        assertEquals("secret", capturedInputParameters.get("hubspot_1")
            .get("apiKey"));
    }
}
