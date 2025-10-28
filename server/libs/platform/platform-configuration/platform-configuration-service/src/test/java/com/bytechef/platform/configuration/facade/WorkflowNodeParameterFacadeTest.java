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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.dto.DisplayConditionResultDTO;
import com.bytechef.platform.configuration.dto.ParameterResultDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Igor Beslic
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowNodeParameterFacadeTest {

    private static final WorkflowNodeParameterFacadeImpl WORKFLOW_NODE_PARAMETER_FACADE =
        new WorkflowNodeParameterFacadeImpl(null, null, SpelEvaluator.create(), null, null, null, null, null);

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeParameterFacadeImpl workflowNodeParameterFacade;

    @BeforeEach
    void setUp() {
        workflowNodeParameterFacade = new WorkflowNodeParameterFacadeImpl(
            actionDefinitionService, clusterElementDefinitionService, evaluator,
            taskDispatcherDefinitionService, triggerDefinitionService, workflowNodeOutputFacade,
            workflowService, workflowTestConfigurationService);
    }

    @Test
    void testDeleteClusterElementParameterSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String path = "param1";

        // Setup ClusterElementDefinition mock directly
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", parameters);
            clusterElementMap.put("metadata", new HashMap<>());

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getVersion()).thenReturn(1);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, false,
                false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).getWorkflow(workflowId);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteClusterElementParameterWithInvalidClusterElement() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "nonexistent";
        String clusterElementWorkflowNodeName = "loopTask";
        String path = "param1";

        // Note: ClusterElementDefinition mock not needed since ConfigurationException is thrown before it's used

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create task definition without the requested cluster element type
            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", new HashMap<>()); // Empty cluster elements

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();
            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When/Then - This should throw a ConfigurationException due to missing cluster element
            assertThrows(ConfigurationException.class, () -> workflowNodeParameterFacade.deleteClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, false,
                false, 0));
        }
    }

    @Test
    void testDeleteClusterElementParameterWithNestedClusterElements() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "nested_loop";
        String clusterElementWorkflowNodeName = "nestedLoopTask";
        String path = "param1";

        // Setup ClusterElementDefinition mock directly
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create nested cluster element structure
            Map<String, Object> nestedLoopParams = new HashMap<>();
            Map<String, Object> nestedLoopElement = new HashMap<>();

            nestedLoopElement.put("name", clusterElementWorkflowNodeName);
            nestedLoopElement.put("type", "loop/v1/loop");
            nestedLoopElement.put("parameters", nestedLoopParams);
            nestedLoopElement.put("metadata", new HashMap<>());

            Map<String, Object> innerClusterElements = new HashMap<>();

            innerClusterElements.put(clusterElementTypeName, nestedLoopElement);

            Map<String, Object> outerLoopElement = new HashMap<>();

            outerLoopElement.put("name", "outerLoop");
            outerLoopElement.put("type", "loop/v1/loop");
            outerLoopElement.put("parameters", new HashMap<>());
            outerLoopElement.put("metadata", new HashMap<>());
            outerLoopElement.put("clusterElements", innerClusterElements);

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put("loop", outerLoopElement);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            List<Map<String, Object>> tasks = new ArrayList<>();
            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getVersion()).thenReturn(1);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, false,
                false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteWorkflowNodeParameterSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String path = "param1";

        // Setup ActionDefinition mock outside of JsonUtils mocking
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getVersion()).thenReturn(1);
            when(workflow.getDefinition()).thenReturn("{}"); // Simple JSON since we're mocking JsonUtils
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
                workflowId, workflowNodeName, path, false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).getWorkflow(workflowId);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteWorkflowNodeParameterWithArrayIndexReordering() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "items[1].value";

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create workflow with array data
            Map<String, Object> parameters = new HashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item1 = new HashMap<>();

            item1.put("value", "item1");

            Map<String, Object> item2 = new HashMap<>();

            item2.put("value", "item2");
            items.add(item1);
            items.add(item2);

            parameters.put("items", items);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getVersion()).thenReturn(1);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteWorkflowNodeParameterNonExistentWorkflowNode() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "nonExistentTask";
        String path = "param1";

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create workflow definition with a different task name
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1"); // Different from nonExistentTask
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When/Then
            assertThrows(ConfigurationException.class,
                () -> workflowNodeParameterFacade.deleteWorkflowNodeParameter(workflowId, workflowNodeName, path, false,
                    0));
        }
    }

    @Test
    void testDeleteWorkflowNodeParameterWithNestedPath() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String path = "nested.param.value";

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getVersion()).thenReturn(1);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
                workflowId, workflowNodeName, path, false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteWorkflowNodeParameterWorkflowNotFound() {
        // Given
        String workflowId = "nonexistent";
        String workflowNodeName = "task1";
        String path = "param1";

        when(workflowService.getWorkflow(workflowId))
            .thenThrow(new RuntimeException("Workflow not found"));

        // When/Then
        assertThrows(RuntimeException.class,
            () -> workflowNodeParameterFacade.deleteWorkflowNodeParameter(workflowId, workflowNodeName, path, false,
                0));
    }

    @Test
    public void testEvaluateArray() {
        Map<String, Object> parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "REGEX"))));

        Map<String, String> displayConditionMap = new HashMap<>();

        WORKFLOW_NODE_PARAMETER_FACADE.evaluateArray(
            "name", "conditions[index][index].operation != 'EMPTY'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        Assertions.assertEquals(2, displayConditionMap.size());
        Assertions.assertEquals(
            Map.of(
                "conditions[0][0].operation != 'EMPTY'", "0_0_name",
                "conditions[1][0].operation != 'EMPTY'", "1_0_name"),
            displayConditionMap);

        displayConditionMap = new HashMap<>();

        WORKFLOW_NODE_PARAMETER_FACADE.evaluateArray(
            "name", "conditions[index][index].operation == 'EMPTY'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        Assertions.assertEquals(1, displayConditionMap.size());
        Assertions.assertEquals(Map.of("conditions[0][1].operation == 'EMPTY'", "0_1_name"), displayConditionMap);

        parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "NOT_CONTAINS"))));

        displayConditionMap = new HashMap<>();

        WORKFLOW_NODE_PARAMETER_FACADE.evaluateArray(
            "name", "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", displayConditionMap, Map.of(),
            Map.of(), parametersMap);

        Assertions.assertEquals(1, displayConditionMap.size());
        Assertions.assertEquals(
            Map.of("!contains({'EMPTY','REGEX'}, conditions[1][0].operation)", "1_0_name"), displayConditionMap);
    }

    @Test
    public void testHasExpressionVariable() {
        String[] expressions = {
            "variableName == 45", "'string' == variableName", "prefixVariableName!= newValue1 && !variableName ",
            "variableNameSuffix!= variableValue1 && !variableName", "prefixVariableName == 44 or variableName lt 45"
        };

        for (String expression : expressions) {
            assertTrue(
                WorkflowNodeParameterFacadeImpl.hasExpressionVariable(expression, "variableName"),
                expression + "doesn't contain variableName");
        }

        String[] noVariableNameExpressions = {
            "prefixVariableName == 45", "'A' == variableNameSuffix", "prefixVariableName!= val && !variableNameSuffix ",
            "variableNameSuffix!= variableValue1 && !prefixVariableName", "prefixVariableName>44 or variableNameS lt 45"
        };

        for (String noVariableExpression : noVariableNameExpressions) {
            assertFalse(
                WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
                    noVariableExpression, "variableName", null),
                noVariableExpression + " doesn't contain variableName");
        }
    }

    @Test
    void testHasExpressionVariableEdgeCases() {
        // Test various edge cases for expression variable detection
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("", "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(null, "param1"));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("param1", "param1"));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("(param1)", "param1"));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("param1.nested", "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("param1nested", "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("myparam1", "param1"));
    }

    @Test
    void testHasExpressionVariableWithIndexesEdgeCases() {
        List<Integer> emptyIndexes = List.of();
        List<Integer> multipleIndexes = List.of(0, 1, 2);

        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("", "param", emptyIndexes));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(null, "param", emptyIndexes));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("no_index_here", "param", emptyIndexes));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "items[index][index][index].value", "items[0][1][2].value", multipleIndexes));
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();
            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
            assertNotNull(result.missingRequiredProperties());
            verify(workflowService).getWorkflow(workflowId);
        }
    }

    @Test
    void testGetClusterElementDisplayConditionsSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";

        // Setup ClusterElementDefinition mock directly
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);
        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", parameters);
            clusterElementMap.put("metadata", new HashMap<>());

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
            assertNotNull(result.missingRequiredProperties());
            verify(workflowService).getWorkflow(workflowId);
        }
    }

    @Test
    public void testEvaluate1() {
        Map<String, Object> parametersMap = Map.of(
            "body", Map.of("bodyContentType", "JSON"));

        boolean result = WORKFLOW_NODE_PARAMETER_FACADE.evaluate(
            "body.bodyContentType == 'JSON'", Map.of(), Map.of(), parametersMap);

        assertTrue(result);

        result = WORKFLOW_NODE_PARAMETER_FACADE.evaluate(
            "body.bodyContentType == 'XML'", Map.of(), Map.of(), parametersMap);

        assertFalse(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEvaluate2() {
        // Given
        Map<String, Object> parameterMap = Map.of("body", Map.of("bodyContentType", "JSON"));
        Map<String, Object> inputMap = Map.of();
        Map<String, Object> outputs = Map.of();
        String displayCondition = "body.bodyContentType == 'JSON'";

        when(evaluator.evaluate(any(Map.class), any(Map.class)))
            .thenReturn(Map.of("body", Map.of("bodyContentType", "JSON")))
            .thenReturn(Map.of("displayCondition", true));

        // When
        boolean result = workflowNodeParameterFacade.evaluate(displayCondition, inputMap, outputs, parameterMap);

        // Then
        assertTrue(result);
        verify(evaluator, times(2)).evaluate(any(Map.class), any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEvaluateMethodWithEvaluatorException() {
        // Given
        Map<String, Object> parameterMap = Map.of("body", Map.of("bodyContentType", "JSON"));
        Map<String, Object> inputMap = Map.of();
        Map<String, Object> outputs = Map.of();
        String displayCondition = "body.bodyContentType == 'JSON'";

        when(evaluator.evaluate(any(Map.class), any(Map.class)))
            .thenThrow(new RuntimeException("Evaluator error"))
            .thenReturn(Map.of("displayCondition", false));

        // When
        boolean result = workflowNodeParameterFacade.evaluate(displayCondition, inputMap, outputs, parameterMap);

        // Then
        assertFalse(result);
        verify(evaluator, times(2)).evaluate(any(Map.class), any(Map.class));
    }

    @Test
    void testHasExpressionVariableStaticMethod() {
        // Test the static method with various scenarios
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "param1 == 'value'", "param1"));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "test.param1 != null", "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "param2 == 'value'", "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            null, "param1"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "", "param1"));
    }

    @Test
    void testHasExpressionVariableWithIndexes() {
        // Test the static method with index parameters
        List<Integer> indexes = List.of(0, 1);

        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "items[index][index].name == 'test'", "items[0][1].name", indexes));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            "items.name == 'test'", "items[0].name", indexes));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
            null, "items[0].name", indexes));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetWorkflowNodeDisplayConditionsMissingRequiredProperties() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();
        Property requiredProperty = mock(Property.class);

        when(requiredProperty.getName()).thenReturn("requiredParam");
        when(requiredProperty.getRequired()).thenReturn(true);

        properties.add(requiredProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);

            List<String> strings = result.missingRequiredProperties();

            assertNotNull(strings);
            assertFalse(strings.isEmpty());
            assertTrue(strings.contains("requiredParam"));
        }
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithComplexProperties() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithComplexProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
        }
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithTrigger() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "manual"; // This will be treated as a trigger

        // Setup TriggerDefinition mock
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create trigger definition map
            Map<String, Object> trigger = new HashMap<>();

            trigger.put("name", "manual");
            trigger.put("type", "manual/v1/manual");
            trigger.put("parameters", new HashMap<>());

            List<Map<String, Object>> triggers = new ArrayList<>();

            triggers.add(trigger);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("triggers", triggers);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
            assertNotNull(result.missingRequiredProperties());
            verify(workflowService).getWorkflow(workflowId);
        }
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithArrayItemInLoopItems() {
        String workflowId = "workflow1";
        String workflowNodeName = "logger1";

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(mock(ActionDefinition.class));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> task = new HashMap<>();
            task.put("name", "loop1");
            task.put("type", "loop/v1");

            List<String> arrayItem = List.of(RandomStringUtils.randomAlphanumeric(20));
            Map<String, Object> parameters = new TreeMap<>();
            parameters.put("items", List.of(arrayItem));

            Map<String, Object> iteratee = new HashMap<>();
            iteratee.put("name", "logger1");
            iteratee.put("type", "logger/v1/info");
            iteratee.put("parameters", Map.of("text", RandomStringUtils.randomAlphanumeric(20)));
            iteratee.put("metadata", new HashMap<>());

            parameters.put("iteratee", List.of(iteratee));

            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterNullValue() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = null;
        String type = null;
        boolean includeInMetadata = false;

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterWithArrayPath() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "items[0].name";
        Object value = "arrayValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();
            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();
            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterTaskDispatcher() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup ActionDefinition for component/v1/action workflow type
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create task dispatcher definition
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "dispatcher/v1/dispatcher");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            // Note: Implementation uses ActionDefinitionService, not TaskDispatcherDefinitionService for this workflow
            // type
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateClusterElementParameterWithMetadataManagement() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "items[0].nested.value";
        Object value = "complexValue";
        String type = "OBJECT";
        boolean includeInMetadata = true;

        // Setup ClusterElementDefinition mock directly
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", parameters);
            clusterElementMap.put("metadata", new HashMap<>());

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                parameterPath, value, type, false, includeInMetadata, 0);

            // Then
            assertNotNull(result);

            Map<String, ?> metadata = result.metadata();

            assertNotNull(metadata);
            assertTrue(metadata.containsKey("ui"));

            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterWithDependentProperties() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "connectionId";
        Object value = "new_connection";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = createMockActionDefinitionWithDependentProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create task definition
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetWorkflowNodeDisplayConditionsWithArrayIndexConditions() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithArrayProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(Map.of());

        // Mock evaluator to return proper boolean values
        when(evaluator.evaluate(any(Map.class), any(Map.class)))
            .thenReturn(Map.of("displayCondition", false));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();

            items.add(Map.of("value", "item1"));
            items.add(Map.of("value", "item2"));
            parameters.put("items", items);

            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            WorkflowTask workflowTask = mock(WorkflowTask.class);

            when(workflowTask.getName()).thenReturn(workflowNodeName);
            when(workflow.getTask(workflowNodeName)).thenReturn(workflowTask);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
        }
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithRequiredNestedProperties() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithRequiredNestedProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
                workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.missingRequiredProperties());
            assertFalse(result.missingRequiredProperties()
                .isEmpty());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
            assertNotNull(result.metadata());
            assertNotNull(result.parameters());
            verify(workflowService).getWorkflow(workflowId);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateClusterElementParameterSuccess() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = false;

        // Setup ClusterElementDefinition mock directly
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", parameters);
            clusterElementMap.put("metadata", new HashMap<>());

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                parameterPath, value, type, false, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            assertNotNull(result.displayConditions());
            assertNotNull(result.metadata());
            assertNotNull(result.parameters());
            verify(workflowService).getWorkflow(workflowId);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testRemoveWorkflowNodeParameterWithNullValue() {
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "nullParamName";
        Object value = null;
        String type = "STRING";
        boolean includeInMetadata = false;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("metadata", new HashMap<>());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("nullParamName", value);

            task.put("parameters", parameters);

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            assertTrue(MapUtils.containsPath(definitionMap, "tasks[0].parameters.nullParamName"));

            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());

            assertFalse(MapUtils.containsPath(definitionMap, "tasks[0].parameters.nullParamName"));
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterWithComplexPath() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "nested.param[0].value";
        Object value = "complexValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterWithTriggerNode() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "triggerNode";
        String parameterPath = "param1";
        Object value = "triggerValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .thenReturn(Map.of());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Create trigger definition
            Map<String, Object> trigger = new HashMap<>();

            trigger.put("name", workflowNodeName);
            trigger.put("type", "webhook/v1/webhook");
            trigger.put("parameters", new HashMap<>());
            trigger.put("metadata", new HashMap<>());

            List<Map<String, Object>> triggers = new ArrayList<>();

            triggers.add(trigger);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("triggers", triggers);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(triggerDefinitionService).getTriggerDefinition(anyString(), anyInt(), anyString());
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateWorkflowNodeParameterMultipleArrayIndexes() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String parameterPath = "matrix[0][1][2].value";
        Object value = "deepArrayValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup ActionDefinition mock directly
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            // Use mutable maps since the implementation modifies them
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> task = new HashMap<>();

            task.put("name", "task1");
            task.put("type", "component/v1/action");
            task.put("parameters", parameters);
            task.put("metadata", new HashMap<>());

            List<Map<String, Object>> tasks = new ArrayList<>();

            tasks.add(task);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", tasks);

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
                workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private ActionDefinition createMockActionDefinitionWithComplexProperties() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        // Create array property with display condition
        ArrayProperty arrayProperty = mock(ArrayProperty.class);

        when(arrayProperty.getName()).thenReturn("items");

        properties.add(arrayProperty);

        // Create object property with display condition
        ObjectProperty objectProperty = mock(ObjectProperty.class);

        when(objectProperty.getName()).thenReturn("config");

        properties.add(objectProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        return actionDefinition;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private ActionDefinition createMockActionDefinitionWithDependentProperties() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        // Create property with options data source that depends on connectionId
        Property dependentProperty = mock(Property.class);
        when(dependentProperty.getName()).thenReturn("tableName");
        properties.add(dependentProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        return actionDefinition;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private ActionDefinition createMockActionDefinitionWithArrayProperties() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty arrayProperty = mock(ArrayProperty.class);
        when(arrayProperty.getName()).thenReturn("conditions");
        when(arrayProperty.getDisplayCondition()).thenReturn("items[index].enabled == true");
        when(arrayProperty.getItems()).thenReturn(List.of());
        properties.add(arrayProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        return actionDefinition;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private ActionDefinition createMockActionDefinitionWithRequiredNestedProperties() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        // Create object property with required nested properties
        ObjectProperty objectProperty = mock(ObjectProperty.class);
        when(objectProperty.getName()).thenReturn("config");
        when(objectProperty.getRequired()).thenReturn(true);

        properties.add(objectProperty);

        // Create array property with required nested properties
        ArrayProperty arrayProperty = mock(ArrayProperty.class);
        when(arrayProperty.getName()).thenReturn("items");
        when(arrayProperty.getRequired()).thenReturn(false);

        properties.add(arrayProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);

        return actionDefinition;
    }

    @Test
    void testGetTaskWithForkJoinNestedLists() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "forkJoinTask";

        // Setup workflow with fork/join structure (List<List<Map>>) using mutable maps
        Map<String, Object> task1 = new HashMap<>();
        task1.put("name", "task1");
        task1.put("type", "component/v1/action");

        Map<String, Object> targetTask = new HashMap<>();
        targetTask.put("name", workflowNodeName);
        targetTask.put("type", "component/v1/action");

        Map<String, Object> task2 = new HashMap<>();
        task2.put("name", "task2");
        task2.put("type", "component/v1/action");

        List<List<Map<String, Object>>> forkJoinTasks = List.of(
            List.of(task1, targetTask),
            List.of(task2));

        Map<String, Object> forkJoinParameters = new HashMap<>();
        forkJoinParameters.put("forks", forkJoinTasks);

        Map<String, Object> forkJoinTask = new HashMap<>();
        forkJoinTask.put("name", "forkJoin");
        forkJoinTask.put("type", "fork/v1");
        forkJoinTask.put("parameters", forkJoinParameters);

        Map<String, Object> definitionMap = new HashMap<>();
        definitionMap.put("tasks", List.of(forkJoinTask));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            // Mock ActionDefinition for the component tasks
            ActionDefinition actionDefinition = mock(ActionDefinition.class);
            when(actionDefinition.getProperties()).thenReturn(List.of());
            when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(actionDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(any(), anyLong()))
                .thenReturn(Map.of());

            // When
            DisplayConditionResultDTO result =
                workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(workflowId, workflowNodeName, 0);

            // Then
            assertNotNull(result);
            // The method should be able to find the nested task within the fork/join structure
        }
    }

    @Test
    void testGetTaskWithNestedClusterElementsInList() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "subtask1";

        // Setup workflow with cluster elements in a list structure using mutable maps
        Map<String, Object> clusterElementParams = new HashMap<>();
        clusterElementParams.put("param1", "value1");

        Map<String, Object> clusterElement = new HashMap<>();
        clusterElement.put("name", clusterElementWorkflowNodeName);
        clusterElement.put("type", "component/v1/action");
        clusterElement.put("parameters", clusterElementParams);

        // Create cluster elements map structure as expected by the implementation
        Map<String, Object> clusterElementsMap = new HashMap<>();
        clusterElementsMap.put(clusterElementTypeName, List.of(clusterElement));

        Map<String, Object> caseMap = new HashMap<>();
        caseMap.put("key", "case1");
        caseMap.put("clusterElements", clusterElementsMap);

        Map<String, Object> taskParameters = new HashMap<>();
        taskParameters.put("cases", List.of(caseMap));

        Map<String, Object> taskWithClusterElements = new HashMap<>();
        taskWithClusterElements.put("name", workflowNodeName);
        taskWithClusterElements.put("type", "branch/v1");
        taskWithClusterElements.put("parameters", taskParameters);
        taskWithClusterElements.put("clusterElements", clusterElementsMap);

        Map<String, Object> definitionMap = new HashMap<>();
        definitionMap.put("tasks", List.of(taskWithClusterElements));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

            when(clusterElementDefinition.getProperties()).thenReturn(List.of());
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);

            // When
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, 0);

            // Then
            assertNotNull(result);
            // The method should be able to find cluster elements within list structures
        }
    }

    @Test
    void testUpdateClusterElementParameterWithNestedStructures() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "branchTask";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "nestedTask";
        String parameterPath = "config.nested.value";
        Object value = "nestedValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        // Setup complex nested structure with cluster elements
        Map<String, Object> clusterElement = new HashMap<>();

        clusterElement.put("name", clusterElementWorkflowNodeName);
        clusterElement.put("type", "component/v1/action");
        clusterElement.put("parameters", new HashMap<>());
        clusterElement.put("metadata", new HashMap<>());

        // Create cluster elements map structure as expected by the implementation
        Map<String, Object> clusterElementsMap = new HashMap<>();

        clusterElementsMap.put(clusterElementTypeName, List.of(clusterElement));

        Map<String, Object> caseMap = new HashMap<>();
        caseMap.put("key", "case1");
        caseMap.put("clusterElements", clusterElementsMap);

        Map<String, Object> taskParameters = new HashMap<>();
        taskParameters.put("cases", List.of(caseMap));

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "branch/v1");
        task.put("parameters", taskParameters);
        task.put("metadata", new HashMap<>());
        task.put("clusterElements", clusterElementsMap);

        Map<String, Object> definitionMap = new HashMap<>();
        definitionMap.put("tasks", List.of(task));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getDefinition()).thenReturn("{}");
            when(workflow.getVersion()).thenReturn(1);
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

            when(clusterElementDefinition.getProperties()).thenReturn(List.of());
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(any(), anyLong()))
                .thenReturn(Map.of());

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                parameterPath, value, type, false, includeInMetadata, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
            // Verify that the parameter was set in the nested structure
        }
    }

    @Test
    void testGetClusterElementDisplayConditionsWithMultipleSiblingNestedRootsSameType() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "taskWithClusters";
        String rootTypeName = "loop";
        String innerTypeName = "case";
        String targetChildName = "childA";

        // Build child cluster elements
        Map<String, Object> childA = new HashMap<>();

        childA.put("name", targetChildName);
        childA.put("type", "component/v1/action");
        childA.put("parameters", new HashMap<>());
        childA.put("metadata", new HashMap<>());

        Map<String, Object> childB = new HashMap<>();

        childB.put("name", "childB");
        childB.put("type", "component/v1/action");
        childB.put("parameters", new HashMap<>());
        childB.put("metadata", new HashMap<>());

        // Build two sibling nested cluster roots of the same type under the task
        Map<String, Object> rootA = new HashMap<>();

        rootA.put("name", "rootA");
        rootA.put("type", "loop/v1/loop");

        Map<String, Object> rootAClusterElements = new HashMap<>();

        rootAClusterElements.put(innerTypeName, List.of(childA));

        rootA.put("clusterElements", rootAClusterElements);

        Map<String, Object> rootB = new HashMap<>();

        rootB.put("name", "rootB");
        rootB.put("type", "loop/v1/loop");

        Map<String, Object> rootBClusterElements = new HashMap<>();

        rootBClusterElements.put(innerTypeName, List.of(childB));

        rootB.put("clusterElements", rootBClusterElements);

        // Put rootB first to ensure traversal hits a sibling without the target child before finding it
        Map<String, Object> topClusterElements = new HashMap<>();

        topClusterElements.put(rootTypeName, List.of(rootB, rootA));

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", new HashMap<>());
        task.put("clusterElements", topClusterElements);

        Map<String, Object> definitionMap = new HashMap<>();
        definitionMap.put("tasks", List.of(task));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

            when(clusterElementDefinition.getProperties()).thenReturn(List.of());
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            // When: request display conditions for a child under one of the siblings
            DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
                workflowId, workflowNodeName, innerTypeName, targetChildName, 0);

            // Then: should not throw and should return a result
            assertNotNull(result);
            assertNotNull(result.displayConditions());
        }
    }

    @Test
    void testDeleteClusterElementParameterWithListStructure() {
        // Given
        String workflowId = "workflow1";
        String workflowNodeName = "conditionalTask";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "listTask";
        String parameterPath = "items.config";

        // Setup cluster elements in list structure using mutable maps
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("key", "value");

        Map<String, Object> itemsMap = new HashMap<>();
        itemsMap.put("config", configMap);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("items", itemsMap);

        Map<String, Object> clusterElement = new HashMap<>();

        clusterElement.put("name", clusterElementWorkflowNodeName);
        clusterElement.put("type", "component/v1/action");
        clusterElement.put("parameters", parameters);
        clusterElement.put("metadata", new HashMap<>());

        // Create cluster elements map structure as expected by the implementation
        Map<String, Object> clusterElementsMap = new HashMap<>();
        clusterElementsMap.put(clusterElementTypeName, List.of(clusterElement));

        Map<String, Object> caseMap = new HashMap<>();

        caseMap.put("key", "case1");
        caseMap.put("clusterElements", clusterElementsMap);

        Map<String, Object> taskParameters = new HashMap<>();

        taskParameters.put("cases", List.of(caseMap));

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "condition/v1");
        task.put("parameters", taskParameters);
        task.put("metadata", new HashMap<>());
        task.put("clusterElements", clusterElementsMap);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", List.of(task));

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getDefinition()).thenReturn("{}");
            when(workflow.getVersion()).thenReturn(1);
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

            when(clusterElementDefinition.getProperties()).thenReturn(List.of());
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);

            // When
            ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, parameterPath,
                false, false, 0);

            // Then
            assertNotNull(result);
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testUpdateClusterElementParameterFromAiInMetadataAddsPath() {
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "items[0].name";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            Map<String, Object> clusterElementMetadata = new HashMap<>();
            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", new HashMap<>());
            clusterElementMap.put("metadata", clusterElementMetadata);

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", List.of(task));

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);
            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                parameterPath, "value", "STRING", true, false, 0);

            assertNotNull(result);

            Map<String, ?> metadata = result.metadata();

            assertNotNull(metadata);

            Object uiObj = metadata.get("ui");

            assertNotNull(uiObj);

            @SuppressWarnings("unchecked")
            List<String> fromAi = (List<String>) ((Map<String, Object>) uiObj).get("fromAi");

            assertNotNull(fromAi);
            assertTrue(fromAi.contains(parameterPath));
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testDeleteClusterElementParameterFromAiInMetadataRemovesPath() {
        String workflowId = "workflow1";
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "config.value";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

        try (MockedStatic<JsonUtils> mockedJsonUtils = mockStatic(JsonUtils.class)) {
            Map<String, Object> ui = new HashMap<>();

            ui.put("fromAi", new ArrayList<>(List.of(parameterPath)));

            Map<String, Object> clusterElementMetadata = new HashMap<>();

            clusterElementMetadata.put("ui", ui);

            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> mutableConfig = new HashMap<>();

            mutableConfig.put("value", "x");

            parameters.put("config", mutableConfig);

            Map<String, Object> clusterElementMap = new HashMap<>();

            clusterElementMap.put("name", clusterElementWorkflowNodeName);
            clusterElementMap.put("type", "loop/v1/loop");
            clusterElementMap.put("parameters", parameters);
            clusterElementMap.put("metadata", clusterElementMetadata);

            Map<String, Object> clusterElements = new HashMap<>();

            clusterElements.put(clusterElementTypeName, clusterElementMap);

            Map<String, Object> task = new HashMap<>();

            task.put("name", workflowNodeName);
            task.put("type", "component/v1/action");
            task.put("parameters", new HashMap<>());
            task.put("metadata", new HashMap<>());
            task.put("clusterElements", clusterElements);

            Map<String, Object> definitionMap = new HashMap<>();

            definitionMap.put("tasks", List.of(task));

            mockedJsonUtils.when(() -> JsonUtils.readMap(anyString()))
                .thenReturn(definitionMap);
            mockedJsonUtils.when(() -> JsonUtils.writeWithDefaultPrettyPrinter(any(), any(Boolean.class)))
                .thenReturn("{}");

            Workflow workflow = mock(Workflow.class);

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getDefinition()).thenReturn("{}");
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
            when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
                .thenReturn(clusterElementDefinition);
            when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
                .thenReturn(Map.of());

            ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
                workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
                parameterPath, true, false, 0);

            assertNotNull(result);

            Map<String, ?> metadata = result.metadata();

            assertNotNull(metadata);

            @SuppressWarnings("unchecked")
            Map<String, Object> uiAfter = (Map<String, Object>) metadata.get("ui");

            assertNotNull(uiAfter);

            @SuppressWarnings("unchecked")
            List<String> fromAi = (List<String>) uiAfter.get("fromAi");

            assertNotNull(fromAi);
            assertFalse(fromAi.contains(parameterPath));
            verify(workflowService).update(anyString(), anyString(), anyInt());
        }
    }
}
