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

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.dto.DisplayConditionResultDTO;
import com.bytechef.platform.configuration.dto.ParameterResultDTO;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.workflow.WorkflowUpdateGuard;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Igor Beslic
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WorkflowNodeParameterFacadeIntTest {

    private static final int CREATED_WORKFLOW_VERSION = 1;

    @Autowired
    private ActionDefinitionService actionDefinitionService;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkflowNodeParameterFacadeImpl workflowNodeParameterFacade;

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @MockitoBean
    private WorkflowUpdateGuard workflowUpdateGuard;

    @AfterEach
    void afterEach() {
        workflowNodeTestOutputRepository.deleteAll();
        workflowTestConfigurationRepository.deleteAll();
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testUpdateClusterElementParameterConsultsWorkflowUpdateGuardAndUpdates() {
        String workflowId = createWorkflow(createClusterElementDefinitionMap());

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
            workflowId, "task1", "loop", "loopTask", "param1", "testValue", "STRING", false, false, 0);

        assertNotNull(result);
        assertEquals(CREATED_WORKFLOW_VERSION + 1, result.version());

        verify(workflowUpdateGuard).checkUpdatable(workflowId);
    }

    @Test
    void testParameterWritesAreRefusedWhenWorkflowUpdateGuardRejects() {
        String workflowId = createWorkflow(createClusterElementDefinitionMap());

        doThrow(new IllegalArgumentException(
            "Workflow %s is generated by an AI agent; edit it through the agent".formatted(workflowId)))
                .when(workflowUpdateGuard)
                .checkUpdatable(workflowId);

        assertThatThrownBy(() -> workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, "task1", "param1", "value", "STRING", false, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("generated by an AI agent");
        assertThatThrownBy(() -> workflowNodeParameterFacade.updateClusterElementParameter(
            workflowId, "task1", "loop", "loopTask", "param1", "value", "STRING", false, false, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, "task1", "param1", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, "task1", "loop", "loopTask", "param1", 0))
                .isInstanceOf(IllegalArgumentException.class);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        assertEquals(CREATED_WORKFLOW_VERSION, workflow.getVersion());
    }

    @Test
    void testDeleteClusterElementParameterFromAiInMetadataRemovesPath() {
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "config.value";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
            parameterPath, 0);

        assertNotNull(result);
        assertEquals(CREATED_WORKFLOW_VERSION + 1, result.version());

        Map<String, ?> metadata = result.metadata();

        assertNotNull(metadata);

        @SuppressWarnings("unchecked")
        Map<String, Object> uiAfter = (Map<String, Object>) metadata.get("ui");

        assertNotNull(uiAfter);

        @SuppressWarnings("unchecked")
        List<String> fromAi = (List<String>) uiAfter.get("fromAi");

        assertNotNull(fromAi);
        assertFalse(fromAi.contains(parameterPath));
        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testDeleteClusterElementParameterSuccess() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String path = "param1";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, 0);

        // Then
        assertNotNull(result);
        assertEquals(CREATED_WORKFLOW_VERSION + 1, result.version());

        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testDeleteClusterElementParameterWithInvalidClusterElement() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "nonexistent";
        String clusterElementWorkflowNodeName = "loopTask";
        String path = "param1";

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

        String workflowId = createWorkflow(definitionMap);

        // When/Then - This should throw a ConfigurationException due to missing cluster element
        assertThrows(ConfigurationException.class, () -> workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, 0));
    }

    @Test
    void testDeleteClusterElementParameterWithListStructure() {
        // Given
        String workflowNodeName = "conditionalTask";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "listTask";
        String parameterPath = "items.config";

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

        String workflowId = createWorkflow(definitionMap);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, parameterPath,
            0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testDeleteClusterElementParameterWithNestedClusterElements() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "nested_loop";
        String clusterElementWorkflowNodeName = "nestedLoopTask";
        String path = "param1";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, path, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterSuccess() {
        // Given
        String workflowNodeName = "task1";
        String path = "param1";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> task = new HashMap<>();

        task.put("name", "task1");
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, path, 0);

        // Then
        assertNotNull(result);

        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterWithArrayIndexReordering() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "items[1].value";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterNonExistentWorkflowNode() {
        // Given
        String workflowNodeName = "nonExistentTask";
        String path = "param1";

        Map<String, Object> task = new HashMap<>();

        task.put("name", "task1");
        task.put("type", "component/v1/action");
        task.put("parameters", new HashMap<>());
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When/Then
        assertThrows(ConfigurationException.class,
            () -> workflowNodeParameterFacade.deleteWorkflowNodeParameter(workflowId, workflowNodeName, path,
                0));
    }

    @Test
    void testDeleteWorkflowNodeParameterWithNestedPath() {
        // Given
        String workflowNodeName = "task1";
        String path = "nested.param.value";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, path, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterWorkflowNotFound() {
        // Given
        String workflowId = "nonexistent";
        String workflowNodeName = "task1";
        String path = "param1";

        // When/Then
        assertThrows(RuntimeException.class,
            () -> workflowNodeParameterFacade.deleteWorkflowNodeParameter(workflowId, workflowNodeName, path, 0));
    }

    @Test
    void testDeleteWorkflowNodeParameterCascadesTransitiveDependents() {
        // Given: A → B → C → D dependency chain. Clearing A must remove B, C, and D
        // so dependent options aren't loaded with stale lookup values (issue #4886).

        String workflowNodeName = "task1";
        String path = "A";

        StringProperty propertyA = mock(StringProperty.class);

        when(propertyA.getName()).thenReturn("A");
        when(propertyA.getOptionsDataSource()).thenReturn(null);

        OptionsDataSource bDataSource = mock(OptionsDataSource.class);

        when(bDataSource.getOptionsLookupDependsOn()).thenReturn(List.of("A"));

        StringProperty propertyB = mock(StringProperty.class);

        when(propertyB.getName()).thenReturn("B");
        when(propertyB.getOptionsDataSource()).thenReturn(bDataSource);

        OptionsDataSource cDataSource = mock(OptionsDataSource.class);

        when(cDataSource.getOptionsLookupDependsOn()).thenReturn(List.of("B"));

        StringProperty propertyC = mock(StringProperty.class);

        when(propertyC.getName()).thenReturn("C");
        when(propertyC.getOptionsDataSource()).thenReturn(cDataSource);

        OptionsDataSource dDataSource = mock(OptionsDataSource.class);

        when(dDataSource.getOptionsLookupDependsOn()).thenReturn(List.of("C"));

        StringProperty propertyD = mock(StringProperty.class);

        when(propertyD.getName()).thenReturn("D");
        when(propertyD.getOptionsDataSource()).thenReturn(dDataSource);

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        doReturn(List.of(propertyA, propertyB, propertyC, propertyD)).when(actionDefinition)
            .getProperties();
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("A", "a-val");
        parameters.put("B", "b-val");
        parameters.put("C", "c-val");
        parameters.put("D", "d-val");

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, path, 0);

        // Then
        Map<String, ?> resultParameters = result.parameters();

        assertFalse(resultParameters.containsKey("A"));
        assertFalse(resultParameters.containsKey("B"));
        assertFalse(resultParameters.containsKey("C"));
        assertFalse(resultParameters.containsKey("D"));
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    public void testEvaluate1() {
        Map<String, Object> parametersMap = Map.of(
            "body", Map.of("bodyContentType", "JSON"));

        boolean result = workflowNodeParameterFacade.evaluate(
            "body.bodyContentType == 'JSON'", Map.of(), Map.of(), parametersMap);

        assertTrue(result);

        result = workflowNodeParameterFacade.evaluate(
            "body.bodyContentType == 'XML'", Map.of(), Map.of(), parametersMap);

        assertFalse(result);
    }

    @Test
    void testEvaluate2() {
        // Given: the parameter is a formula that only resolves against the previous node outputs
        Map<String, Object> parameterMap = Map.of("body", Map.of("bodyContentType", "=source.contentType"));
        Map<String, Object> inputMap = Map.of();
        Map<String, Object> outputs = Map.of("source", Map.of("contentType", "JSON"));
        String displayCondition = "body.bodyContentType == 'JSON'";

        // When
        boolean result = workflowNodeParameterFacade.evaluate(displayCondition, inputMap, outputs, parameterMap);

        // Then
        assertTrue(result);
        assertFalse(
            workflowNodeParameterFacade.evaluate(
                displayCondition, inputMap, Map.of("source", Map.of("contentType", "XML")), parameterMap));
    }

    @Test
    public void testEvaluateArray() {
        Map<String, Object> parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "REGEX"))));

        Map<String, String> displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "name", "conditions[index][index].operation != 'EMPTY'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        assertEquals(2, displayConditionMap.size());
        assertEquals(
            Map.of(
                "conditions[0][0].operation != 'EMPTY'", "0_0_name",
                "conditions[1][0].operation != 'EMPTY'", "1_0_name"),
            displayConditionMap);

        displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "name", "conditions[index][index].operation == 'EMPTY'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        assertEquals(1, displayConditionMap.size());
        assertEquals(Map.of("conditions[0][1].operation == 'EMPTY'", "0_1_name"), displayConditionMap);

        parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "NOT_CONTAINS"))));

        displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "name", "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", displayConditionMap, Map.of(),
            Map.of(), parametersMap);

        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("!contains({'EMPTY','REGEX'}, conditions[1][0].operation)", "1_0_name"), displayConditionMap);
    }

    @Test
    public void testEvaluateArraySingleLevel() {
        // Test single-level array displayCondition like list[index].type == 'ARRAY'
        Map<String, Object> parametersMap = Map.of(
            "list",
            List.of(
                Map.of("key", "foo", "type", "ARRAY"),
                Map.of("key", "bar", "type", "STRING")));

        Map<String, String> displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "value", "list[index].type == 'ARRAY'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("list[0].type == 'ARRAY'", "0_value"),
            displayConditionMap);

        // Test STRING type
        displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "value", "list[index].type == 'STRING'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("list[1].type == 'STRING'", "1_value"),
            displayConditionMap);
    }

    @Test
    public void testEvaluateArrayMultiLevelNested() {
        Map<String, Object> parametersMap = Map.of(
            "CHILDREN", List.of(
                Map.of("caption", List.of(
                    Map.of("TYPE", "text"),
                    Map.of("TYPE", "image"))),
                Map.of("caption", List.of(
                    Map.of("TYPE", "video")))));

        Map<String, String> displayConditionMap = new HashMap<>();

        workflowNodeParameterFacade.evaluateArray(
            "prop", "CHILDREN[index].caption[index].TYPE == 'text'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);

        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("CHILDREN[0].caption[0].TYPE == 'text'", "0_0_prop"),
            displayConditionMap);

        displayConditionMap = new HashMap<>();
        workflowNodeParameterFacade.evaluateArray(
            "prop", "CHILDREN[index].caption[index].TYPE == 'image'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);
        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("CHILDREN[0].caption[1].TYPE == 'image'", "0_1_prop"),
            displayConditionMap);

        displayConditionMap = new HashMap<>();
        workflowNodeParameterFacade.evaluateArray(
            "prop", "CHILDREN[index].caption[index].TYPE == 'video'", displayConditionMap, Map.of(), Map.of(),
            parametersMap);
        assertEquals(1, displayConditionMap.size());
        assertEquals(
            Map.of("CHILDREN[1].caption[0].TYPE == 'video'", "1_0_prop"),
            displayConditionMap);
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
    void testHasExpressionVariableIgnoresStringLiterals() {
        // Test that parameter names inside string literals are not matched (GitHub issue #3964)
        // When option value equals parameter name, should not match the string literal

        // Should return false when parameter name only appears inside single-quoted string
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("otherParam == 'type'", "type"));
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("field == 'value'", "value"));

        // Should return false when parameter name only appears inside double-quoted string
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("otherParam == \"type\"", "type"));

        // Should return true when parameter name appears as variable (left side of comparison)
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("type == 'type'", "type"));
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("value == 'someValue'", "value"));

        // Should return true when parameter name appears as variable even with same value in string
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("param == 'param'", "param"));

        // Should return false when parameter name only appears in the string value part
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("someField == 'paramName'", "paramName"));

        // Should handle escaped quotes in string literals
        assertFalse(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("field == 'it\\'s'", "s"));
        assertFalse(
            WorkflowNodeParameterFacadeImpl.hasExpressionVariable("field == \"He said \\\"hello\\\"\"", "hello"));

        // Should still match variables outside escaped quote strings
        assertTrue(WorkflowNodeParameterFacadeImpl.hasExpressionVariable("hello == 'He said \\\"hello\\\"'", "hello"));
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
    void testEvaluateMethodWithEvaluatorException() {
        // Given: a sibling parameter calls a method the evaluator does not allow, so evaluating the parameters throws
        // even in lenient mode and the display condition must fall back to the raw parameter values
        Map<String, Object> parameterMap = Map.of(
            "body", Map.of("bodyContentType", "JSON"), "lookup", "=forbiddenMethod()");
        Map<String, Object> inputMap = Map.of();
        Map<String, Object> outputs = Map.of();

        // When
        boolean result = workflowNodeParameterFacade.evaluate(
            "body.bodyContentType == 'JSON'", inputMap, outputs, parameterMap);

        // Then
        assertTrue(result);
        assertFalse(
            workflowNodeParameterFacade.evaluate("body.bodyContentType == 'XML'", inputMap, outputs, parameterMap));
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
    void testGetClusterElementDisplayConditionsSuccess() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);
        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
        assertNotNull(result.missingRequiredProperties());

    }

    @Test
    void testGetClusterElementDisplayConditionsWithMultipleSiblingNestedRootsSameType() {
        // Given
        String workflowNodeName = "taskWithClusters";
        String rootTypeName = "loop";
        String innerTypeName = "case";
        String targetChildName = "childA";

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

        String workflowId = createWorkflow(definitionMap);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When: request display conditions for a child under one of the siblings
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
            workflowId, workflowNodeName, innerTypeName, targetChildName, 0);

        // Then: should not throw and should return a result
        assertNotNull(result);
        assertNotNull(result.displayConditions());
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsReturnsOnlyConditionsThatHold() {
        List<StringProperty> properties = List.of(
            mockRequiredStringProperty("bodyContentJson", "bodyContentType == 'JSON'"),
            mockRequiredStringProperty("bodyContentXml", "bodyContentType == 'XML'"));

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        when(actionDefinitionService.getActionDefinition("httpClient", 1, "post")).thenReturn(actionDefinition);

        Map<String, Boolean> displayConditions = workflowNodeParameterFacade.getDisplayConditions(
            "httpClient", 1, "post", WorkflowNodeParameterFacade.OperationType.ACTION,
            Map.of("bodyContentType", "JSON"));

        assertTrue(displayConditions.get("bodyContentType == 'JSON'"));
        assertFalse(displayConditions.containsKey("bodyContentType == 'XML'"));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsWithoutParametersEvaluatesNothing() {
        List<StringProperty> properties = List.of(
            mockRequiredStringProperty("bodyContentJson", "bodyContentType == 'JSON'"));

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties())
            .thenReturn((List) properties);
        when(actionDefinitionService.getActionDefinition("httpClient", 1, "post")).thenReturn(actionDefinition);

        Map<String, Boolean> displayConditions = workflowNodeParameterFacade.getDisplayConditions(
            "httpClient", 1, "post", WorkflowNodeParameterFacade.OperationType.ACTION, Map.of());

        assertTrue(displayConditions.isEmpty());
    }

    @Test
    void testGetTaskWithForkJoinNestedLists() {
        // Given
        String workflowNodeName = "forkJoinTask";

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

        String workflowId = createWorkflow(definitionMap);

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(List.of());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // The previous outputs of the target task include the enclosing fork task dispatcher
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("fork", 1))
            .thenReturn(mock(TaskDispatcherDefinition.class));

        // When
        DisplayConditionResultDTO result =
            workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetTaskWithNestedClusterElementsInList() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "subtask1";

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

        String workflowId = createWorkflow(definitionMap);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getClusterElementDisplayConditions(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, 0);

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsSuccess() {
        // Given
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
        assertNotNull(result.missingRequiredProperties());

    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetWorkflowNodeDisplayConditionsMissingRequiredProperties() {
        // Given
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

        String workflowId = createWorkflow(definitionMap);

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

    /**
     * Regression for issue #5363: a required property whose display condition is not met (because the referenced field
     * is left at its non-persisted default value) is hidden in the editor and must not be reported as a missing
     * required property. Mirrors the AI Agent Chat action: 'messages' (display condition {@code format == 'ADVANCED'})
     * and 'response.responseSchema' (display condition {@code response.responseFormat == 'JSON'}) with Format=Simple
     * and Response Format=Text.
     */
    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetWorkflowNodeMissingRequiredPropertiesExcludesHiddenProperties() {
        ArrayProperty messages = mock(ArrayProperty.class);

        when(messages.getName())
            .thenReturn("messages");
        when(messages.getRequired())
            .thenReturn(true);
        when(messages.getDisplayCondition())
            .thenReturn("format == 'ADVANCED'");
        when(messages.getItems())
            .thenReturn(List.of());

        StringProperty responseFormat = mockRequiredStringProperty("responseFormat", null);
        StringProperty responseSchema = mockRequiredStringProperty(
            "responseSchema", "response.responseFormat == 'JSON'");
        ObjectProperty response = mock(ObjectProperty.class);

        when(response.getName())
            .thenReturn("response");
        when(response.getRequired())
            .thenReturn(true);
        when(response.getProperties())
            .thenReturn((List) List.of(responseFormat, responseSchema));

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("format", "SIMPLE");
        parameters.put("response", new HashMap<>(Map.of("responseFormat", "TEXT")));

        List<String> missingRequiredProperties = getWorkflowNodeDisplayConditions(
            List.of(mockRequiredStringProperty("format", null), messages, response), parameters)
                .missingRequiredProperties();

        assertFalse(missingRequiredProperties.contains("messages"));
        assertFalse(missingRequiredProperties.contains("response.responseSchema"));
        assertTrue(missingRequiredProperties.isEmpty());
    }

    /**
     * Regression for issue #5363 (second part): clearing a required field in the editor persists an empty string rather
     * than removing the parameter key, so a required property whose value is present but blank must still be reported
     * as missing — otherwise the error never reappears after the user deletes the value.
     */
    @Test
    void testGetWorkflowNodeMissingRequiredPropertiesReportsBlankRequiredValue() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("name", "");

        DisplayConditionResultDTO result = getWorkflowNodeDisplayConditions(
            List.of(mockRequiredStringProperty("name", null)), parameters);

        assertTrue(result.missingRequiredProperties()
            .contains("name"));
    }

    /**
     * Resolving display conditions/missing required properties for a node that has no previous outputs to fetch must
     * not throw, and the required property is still reported missing. A persisted {@link Workflow} never returns null
     * from {@code getTask()}, so the first node of the workflow is the real-data equivalent of a node without previous
     * outputs.
     */
    @Test
    void testGetWorkflowNodeDisplayConditionsWithoutMatchingWorkflowTask() {
        DisplayConditionResultDTO result = getWorkflowNodeDisplayConditions(
            List.of(mockRequiredStringProperty("requiredParam", null)), new HashMap<>());

        assertTrue(result.missingRequiredProperties()
            .contains("requiredParam"));
        verify(actionDefinitionService, times(0)).isDynamicOutputDefined(anyString(), anyInt(), anyString());
    }

    private static StringProperty mockRequiredStringProperty(String name, String displayCondition) {
        StringProperty property = mock(StringProperty.class);

        when(property.getName())
            .thenReturn(name);
        when(property.getRequired())
            .thenReturn(true);
        when(property.getDisplayCondition())
            .thenReturn(displayCondition);

        return property;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private DisplayConditionResultDTO getWorkflowNodeDisplayConditions(
        List<?> properties, Map<String, Object> parameters) {

        String workflowNodeName = "task1";
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        String workflowId = createWorkflow(Map.of("tasks", List.of(task)));

        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        assertNotNull(result);
        assertNotNull(result.missingRequiredProperties());

        return result;
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithComplexProperties() {
        // Given
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithComplexProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithTrigger() {
        // Given
        String workflowNodeName = "manual";

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", "manual");
        trigger.put("type", "manual/v1/manual");
        trigger.put("parameters", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
        assertNotNull(result.missingRequiredProperties());

    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithArrayItemInLoopItems() {
        String workflowNodeName = "logger1";

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(mock(ActionDefinition.class));

        // The previous outputs of the iteratee include the enclosing loop task dispatcher
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(mock(TaskDispatcherDefinition.class));

        Map<String, Object> task = new HashMap<>();
        task.put("name", "loop1");
        task.put("type", "loop/v1");

        List<String> arrayItem = List.of(secure().nextAlphanumeric(20));
        Map<String, Object> parameters = new TreeMap<>();
        parameters.put("items", List.of(arrayItem));

        Map<String, Object> iteratee = new HashMap<>();
        iteratee.put("name", "logger1");
        iteratee.put("type", "logger/v1/info");
        iteratee.put("parameters", Map.of("text", secure().nextAlphanumeric(20)));
        iteratee.put("metadata", new HashMap<>());

        parameters.put("iteratee", List.of(iteratee));

        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetWorkflowNodeDisplayConditionsWithArrayIndexConditions() {
        // Given
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithArrayProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsWithRequiredNestedProperties() {
        // Given
        String workflowNodeName = "task1";

        ActionDefinition actionDefinition = createMockActionDefinitionWithRequiredNestedProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        DisplayConditionResultDTO result = workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(
            workflowId, workflowNodeName, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.missingRequiredProperties());
        assertFalse(result.missingRequiredProperties()
            .isEmpty());
    }

    @Test
    void testUpdateClusterElementParameterWithMetadataManagement() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "items[0].nested.value";
        Object value = "complexValue";
        String type = "OBJECT";
        boolean includeInMetadata = true;

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
            parameterPath, value, type, false, includeInMetadata, 0);

        // Then
        assertNotNull(result);

        Map<String, ?> metadata = result.metadata();

        assertNotNull(metadata);
        assertTrue(metadata.containsKey("ui"));

        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testUpdateClusterElementParameterFromAiInMetadataAddsPath() {
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "items[0].name";

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

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
        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testUpdateClusterElementParameterSuccess() {
        // Given
        String workflowNodeName = "task1";
        String clusterElementTypeName = "loop";
        String clusterElementWorkflowNodeName = "loopTask";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = false;

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
            parameterPath, value, type, false, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
        assertNotNull(result.metadata());
        assertNotNull(result.parameters());

        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @Test
    void testUpdateClusterElementParameterWithNestedStructures() {
        // Given
        String workflowNodeName = "branchTask";
        String clusterElementTypeName = "case";
        String clusterElementWorkflowNodeName = "nestedTask";
        String parameterPath = "config.nested.value";
        Object value = "nestedValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        Map<String, Object> clusterElement = new HashMap<>();

        clusterElement.put("name", clusterElementWorkflowNodeName);
        clusterElement.put("type", "component/v1/action");
        clusterElement.put("parameters", new HashMap<>());
        clusterElement.put("metadata", new HashMap<>());

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

        String workflowId = createWorkflow(definitionMap);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(clusterElementDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateClusterElementParameter(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName,
            parameterPath, value, type, false, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, clusterElementWorkflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    void testUpdateWorkflowNodeParameterCompoundDisplayConditionEvaluated() {
        String workflowNodeName = "formTrigger";
        String parameterPath = "inputs[0].multipleChoice";
        Object value = true;
        String type = "BOOLEAN";
        boolean includeInMetadata = false;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty inputsArrayProperty = mock(ArrayProperty.class);

        when(inputsArrayProperty.getName()).thenReturn("inputs");
        when(inputsArrayProperty.getDisplayCondition())
            .thenReturn(null);

        ObjectProperty inputObjectProperty = mock(ObjectProperty.class);

        when(inputObjectProperty.getName()).thenReturn(null);

        List<Property> inputObjectProperties = new ArrayList<>();

        Property fieldTypeProperty = mock(Property.class);

        when(fieldTypeProperty.getName())
            .thenReturn("fieldType");
        when(fieldTypeProperty.getDisplayCondition())
            .thenReturn(null);

        inputObjectProperties.add(fieldTypeProperty);

        Property multipleChoiceProperty = mock(Property.class);

        when(multipleChoiceProperty.getName()).thenReturn("multipleChoice");
        when(multipleChoiceProperty.getDisplayCondition()).thenReturn("inputs[index].fieldType == 7");

        inputObjectProperties.add(multipleChoiceProperty);

        Property minSelectionProperty = mock(Property.class);

        when(minSelectionProperty.getName()).thenReturn("minSelection");
        when(minSelectionProperty.getDisplayCondition())
            .thenReturn("inputs[index].fieldType == 7 and inputs[index].multipleChoice == true");

        inputObjectProperties.add(minSelectionProperty);

        when(inputObjectProperty.getProperties()).thenReturn((List) inputObjectProperties);

        when(inputsArrayProperty.getItems()).thenReturn((List) List.of(inputObjectProperty));

        properties.add(inputsArrayProperty);

        when(triggerDefinition.getProperties()).thenReturn((List) properties);
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> inputItem = new HashMap<>();

        inputItem.put("fieldType", 7); // SELECT
        inputItem.put("multipleChoice", false);

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(inputItem);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("inputs", inputs);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "form/v1/newFormRequest");
        trigger.put("parameters", parameters);
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, Boolean> displayConditions = result.displayConditions();

        assertTrue(displayConditions.containsKey("inputs[0].fieldType == 7"),
            "Simple display condition 'inputs[0].fieldType == 7' should be in displayConditions");
        assertTrue(displayConditions.containsKey("inputs[0].fieldType == 7 and inputs[0].multipleChoice == true"),
            "Compound display condition should be in displayConditions");

        verify(triggerDefinitionService).getTriggerDefinition(anyString(), anyInt(), anyString());
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    void testUpdateWorkflowNodeParameterCompoundDisplayConditionWithMultipleArrayItems() {
        // Given - Testing compound display condition with multiple array items
        // Ensures that each array item gets its own display condition evaluated correctly

        String workflowNodeName = "formTrigger";
        String parameterPath = "inputs[1].multipleChoice";
        Object value = true;
        String type = "BOOLEAN";
        boolean includeInMetadata = false;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty inputsArrayProperty = mock(ArrayProperty.class);

        when(inputsArrayProperty.getName()).thenReturn("inputs");
        when(inputsArrayProperty.getDisplayCondition())
            .thenReturn(null);

        ObjectProperty inputObjectProperty = mock(ObjectProperty.class);

        when(inputObjectProperty.getName()).thenReturn(null);

        List<Property> inputObjectProperties = new ArrayList<>();

        Property fieldTypeProperty = mock(Property.class);

        when(fieldTypeProperty.getName())
            .thenReturn("fieldType");
        when(fieldTypeProperty.getDisplayCondition())
            .thenReturn(null);

        inputObjectProperties.add(fieldTypeProperty);

        Property minSelectionProperty = mock(Property.class);

        when(minSelectionProperty.getName()).thenReturn("minSelection");
        when(minSelectionProperty.getDisplayCondition())
            .thenReturn("inputs[index].fieldType == 7 and inputs[index].multipleChoice == true");

        inputObjectProperties.add(minSelectionProperty);

        when(inputObjectProperty.getProperties()).thenReturn((List) inputObjectProperties);
        when(inputsArrayProperty.getItems()).thenReturn((List) List.of(inputObjectProperty));

        properties.add(inputsArrayProperty);

        when(triggerDefinition.getProperties()).thenReturn((List) properties);
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> inputItem0 = new HashMap<>();

        inputItem0.put("fieldType", 6); // INPUT - doesn't match condition
        inputItem0.put("multipleChoice", false);

        Map<String, Object> inputItem1 = new HashMap<>();

        inputItem1.put("fieldType", 7); // SELECT - matches condition
        inputItem1.put("multipleChoice", false);

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(inputItem0);
        inputs.add(inputItem1);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("inputs", inputs);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "form/v1/newFormRequest");
        trigger.put("parameters", parameters);
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, Boolean> displayConditions = result.displayConditions();

        assertTrue(
            displayConditions.containsKey("inputs[1].fieldType == 7 and inputs[1].multipleChoice == true"),
            "Compound condition for inputs[1] should be in displayConditions. Actual: " + displayConditions);
        assertFalse(
            displayConditions.containsKey("inputs[0].fieldType == 7 and inputs[0].multipleChoice == true"),
            "Compound condition for inputs[0] should NOT be in displayConditions (evaluates to false). " +
                "Actual: " + displayConditions);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    void testUpdateWorkflowNodeParameterDisplayConditionEvaluatedForArrayItem() {
        // Given - Testing display condition evaluation for array items in form trigger
        // When updating inputs[0].fieldType from 6 (INPUT) to 8 (EMAIL_INPUT),
        // the display condition "inputs[index].fieldType != 12" for fieldName should evaluate to true
        // and be included in displayConditions as "inputs[0].fieldType != 12: true"

        String workflowNodeName = "formTrigger";
        String parameterPath = "inputs[0].fieldType";
        Object value = 8; // EMAIL_INPUT
        String type = "INTEGER";
        boolean includeInMetadata = true;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty inputsArrayProperty = mock(ArrayProperty.class);

        when(inputsArrayProperty.getName()).thenReturn("inputs");
        when(inputsArrayProperty.getDisplayCondition()).thenReturn(null);

        ObjectProperty inputObjectProperty = mock(ObjectProperty.class);

        when(inputObjectProperty.getName()).thenReturn(null); // Array items typically have no name

        List<Property> inputObjectProperties = new ArrayList<>();

        Property fieldTypeProperty = mock(Property.class);

        when(fieldTypeProperty.getName())
            .thenReturn("fieldType");
        when(fieldTypeProperty.getDisplayCondition())
            .thenReturn(null);

        inputObjectProperties.add(fieldTypeProperty);

        Property fieldNameProperty = mock(Property.class);

        when(fieldNameProperty.getName()).thenReturn("fieldName");
        when(fieldNameProperty.getDisplayCondition()).thenReturn("inputs[index].fieldType != 12");

        inputObjectProperties.add(fieldNameProperty);

        when(inputObjectProperty.getProperties()).thenReturn((List) inputObjectProperties);

        when(inputsArrayProperty.getItems()).thenReturn((List) List.of(inputObjectProperty));

        properties.add(inputsArrayProperty);

        when(triggerDefinition.getProperties()).thenReturn((List) properties);
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> inputItem = new HashMap<>();

        inputItem.put("fieldType", 6); // INPUT
        inputItem.put("fieldName", "myField");

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(inputItem);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("inputs", inputs);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "form/v1/newFormRequest");
        trigger.put("parameters", parameters);
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, Boolean> displayConditions = result.displayConditions();

        assertTrue(
            displayConditions.containsKey("inputs[0].fieldType != 12"),
            "Display condition 'inputs[0].fieldType != 12' should be evaluated and included in displayConditions. "
                +
                "Actual displayConditions: " + displayConditions);

        assertTrue(
            displayConditions.get("inputs[0].fieldType != 12"),
            "Display condition 'inputs[0].fieldType != 12' should evaluate to true since fieldType (8) != 12");

        verify(triggerDefinitionService).getTriggerDefinition(anyString(), anyInt(), anyString());
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    @SuppressWarnings({
        "checkstyle:methodlengthcheck", "rawtypes", "unchecked"
    })
    void testUpdateWorkflowNodeParameterDisplayConditionNotEvaluatedForWrongType() {
        // Given - Testing bug where display conditions for NUMBER type properties are evaluated when updating STRING
        // type parameters.
        String workflowNodeName = "condition_1";
        String parameterPath = "conditions[0][0].value2";
        Object value = "value1";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        Property rawExpressionProperty = mock(Property.class);

        when(rawExpressionProperty.getName()).thenReturn("rawExpression");

        when(rawExpressionProperty.getType())
            .thenReturn(com.bytechef.component.definition.Property.Type.BOOLEAN);

        properties.add(rawExpressionProperty);

        ArrayProperty conditionsArrayProperty = mock(ArrayProperty.class);

        when(conditionsArrayProperty.getName()).thenReturn("conditions");
        when(conditionsArrayProperty.getDisplayCondition()).thenReturn("rawExpression == false");

        // Mock the array items - these represent the object variants (boolean, number, string, etc.)
        List<Property> arrayItems = new ArrayList<>();

        // NUMBER variant object with its properties
        // These properties should NOT be used - the filtering should exclude them
        ObjectProperty numberObjectProperty = mock(ObjectProperty.class);

        when(numberObjectProperty.getName())
            .thenReturn("number");

        List<Property> numberProperties = new ArrayList<>();
        Property numberValue2Property = mock(Property.class);

        when(numberValue2Property.getName())
            .thenReturn("value2");
        when(numberValue2Property.getDisplayCondition())
            .thenReturn("conditions[0][0].operation != 'EMPTY'");
        when(numberValue2Property.getType())
            .thenReturn(com.bytechef.component.definition.Property.Type.NUMBER);

        numberProperties.add(numberValue2Property);

        when(numberObjectProperty.getProperties())
            .thenReturn((List) numberProperties);

        arrayItems.add(numberObjectProperty);

        ObjectProperty stringObjectProperty = mock(ObjectProperty.class);

        when(stringObjectProperty.getName()).thenReturn("string");

        List<Property> stringProperties = new ArrayList<>();
        Property stringValue2Property = mock(Property.class);

        when(stringValue2Property.getName()).thenReturn("value2");
        when(stringValue2Property.getDisplayCondition())
            .thenReturn("!contains({'EMPTY','REGEX'}, conditions[0][0].operation)");

        when(stringValue2Property.getType())
            .thenReturn(com.bytechef.component.definition.Property.Type.STRING);

        stringProperties.add(stringValue2Property);

        when(stringObjectProperty.getProperties()).thenReturn((List) stringProperties);
        arrayItems.add(stringObjectProperty);

        when(conditionsArrayProperty.getItems()).thenReturn((List) arrayItems);

        properties.add(conditionsArrayProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> conditionItem = new HashMap<>();

        conditionItem.put("operation", "CONTAINS");
        conditionItem.put("value1", "test");

        List<List<Map<String, Object>>> conditions = new ArrayList<>();

        conditions.add(List.of(conditionItem));

        parameters.put("conditions", conditions);
        parameters.put("rawExpression", false);

        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> ui = new HashMap<>();
        Map<String, String> dynamicPropertyTypes = new HashMap<>();

        // In reality, the metadata stores types for individual properties, not the parent object
        // The parent type "conditions[0][0]" is inferred from child properties
        // Simulate that value1 was already set as STRING (from a previous update)
        dynamicPropertyTypes.put("conditions[0][0].value1", "STRING");
        dynamicPropertyTypes.put("conditions[0]", "ARRAY");

        ui.put("dynamicPropertyTypes", dynamicPropertyTypes);

        metadata.put("ui", ui);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "condition/v1/condition");
        task.put("parameters", parameters);
        task.put("metadata", metadata);

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, ?> metadata1 = result.metadata();

        assertNotNull(metadata1);

        Map<String, Boolean> displayConditions = result.displayConditions();

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMetadata = (Map<String, Object>) metadata1.get("ui");

        if (resultMetadata != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> resultDynamicTypes = (Map<String, String>) resultMetadata.get(
                "dynamicPropertyTypes");

            assertNotNull(resultDynamicTypes);
            assertEquals(
                "STRING", resultDynamicTypes.get("conditions[0][0].value2"),
                "Metadata should be updated with the new property type");
        }

        assertFalse(
            displayConditions.containsKey("conditions[0][0].operation != 'EMPTY'"),
            "NUMBER type display condition should NOT be evaluated for STRING type parameter");
        assertTrue(
            displayConditions.containsKey("!contains({'EMPTY','REGEX'}, conditions[0][0].operation)"),
            "STRING type display condition should be evaluated for STRING type parameter");
        assertTrue(
            displayConditions.containsKey("rawExpression == false"),
            "Parent array display condition should be evaluated");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    void testUpdateWorkflowNodeParameterDisplayConditionNotIncludedWhenFalse() {
        // Given - Testing that when the display condition evaluates to false, it's NOT included in displayConditions
        // When updating inputs[0].fieldType from 8 (EMAIL_INPUT) to 12 (CUSTOM_HTML), the display condition
        // "inputs[index].fieldType != 12" should evaluate to false and should NOT be included in displayConditions

        String workflowNodeName = "formTrigger";
        String parameterPath = "inputs[0].fieldType";
        Object value = 12; // CUSTOM_HTML - same as the value in the condition
        String type = "INTEGER";
        boolean includeInMetadata = true;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty inputsArrayProperty = mock(ArrayProperty.class);

        when(inputsArrayProperty.getName()).thenReturn("inputs");
        when(inputsArrayProperty.getDisplayCondition())
            .thenReturn(null);

        ObjectProperty inputObjectProperty = mock(ObjectProperty.class);

        when(inputObjectProperty.getName()).thenReturn(null);

        List<Property> inputObjectProperties = new ArrayList<>();

        Property fieldTypeProperty = mock(Property.class);

        when(fieldTypeProperty.getName())
            .thenReturn("fieldType");
        when(fieldTypeProperty.getDisplayCondition())
            .thenReturn(null);

        inputObjectProperties.add(fieldTypeProperty);

        Property fieldNameProperty = mock(Property.class);

        when(fieldNameProperty.getName()).thenReturn("fieldName");
        when(fieldNameProperty.getDisplayCondition()).thenReturn("inputs[index].fieldType != 12");

        inputObjectProperties.add(fieldNameProperty);

        when(inputObjectProperty.getProperties()).thenReturn((List) inputObjectProperties);

        when(inputsArrayProperty.getItems()).thenReturn((List) List.of(inputObjectProperty));

        properties.add(inputsArrayProperty);

        when(triggerDefinition.getProperties()).thenReturn((List) properties);
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> inputItem = new HashMap<>();

        inputItem.put("fieldType", 8); // EMAIL_INPUT - will be updated to 12
        inputItem.put("fieldName", "myField");

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(inputItem);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("inputs", inputs);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "form/v1/newFormRequest");
        trigger.put("parameters", parameters);
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, Boolean> displayConditions = result.displayConditions();

        assertFalse(
            displayConditions.containsKey("inputs[0].fieldType != 12"),
            "Display condition 'inputs[0].fieldType != 12' should NOT be included in displayConditions " +
                "when it evaluates to false. Actual displayConditions: " + displayConditions);

        verify(triggerDefinitionService).getTriggerDefinition(anyString(), anyInt(), anyString());
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterMultipleArrayIndexes() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "matrix[0][1][2].value";
        Object value = "deepArrayValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterNullValue() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = null;
        String type = null;
        boolean includeInMetadata = false;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterSuccess() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());
        assertNotNull(result.metadata());
        assertNotNull(result.parameters());

        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterTaskDispatcher() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "param1";
        Object value = "testValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        when(taskDispatcherDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(taskDispatcherDefinition.getName()).thenReturn("loop");
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(taskDispatcherDefinition);

        // Create task dispatcher definition
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> task = new HashMap<>();

        task.put("name", "task1");
        task.put("type", "loop/v1");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
        verify(taskDispatcherDefinitionService).getTaskDispatcherDefinition(anyString(), anyInt());
    }

    @Test
    void testUpdateWorkflowNodeParameterBranchTaskDispatcherDoesNotRemoveEmptyCollections() {
        // Given
        String workflowNodeName = "branch1";
        String parameterPath = "expression";
        Object value = "true";
        String type = "STRING";
        boolean includeInMetadata = true;

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        when(taskDispatcherDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(taskDispatcherDefinition.getName()).thenReturn("branch");
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("branch", 1))
            .thenReturn(taskDispatcherDefinition);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("cases", new ArrayList<>()); // Empty list

        Map<String, Object> task = new HashMap<>();

        task.put("name", "branch1");
        task.put("type", "branch/v1");
        task.put("parameters", parameters);

        String workflowId = createWorkflow(Map.of("tasks", List.of(task)));

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertTrue(
            getPersistedParameters(workflowId, workflowNodeName).containsKey("cases"),
            "Empty 'cases' list should NOT be removed for branch");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterOtherTaskDispatcherRemovesEmptyCollections() {
        // Given
        String workflowNodeName = "loop1";
        String parameterPath = "iterable";
        Object value = "[1, 2]";
        String type = "STRING";
        boolean includeInMetadata = true;

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        when(taskDispatcherDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(taskDispatcherDefinition.getName()).thenReturn("loop");
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(taskDispatcherDefinition);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("emptyList", new ArrayList<>()); // Empty list

        Map<String, Object> task = new HashMap<>();

        task.put("name", "loop1");
        task.put("type", "loop/v1");
        task.put("parameters", parameters);

        String workflowId = createWorkflow(Map.of("tasks", List.of(task)));

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertFalse(
            getPersistedParameters(workflowId, workflowNodeName).containsKey("emptyList"),
            "Empty list should be removed for other dispatchers");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    void testUpdateWorkflowNodeParameterTripleCompoundDisplayCondition() {
        // Given - Testing compound display condition with three same-level array references
        // e.g., "inputs[index].a == 1 and inputs[index].b == 2 and inputs[index].c == 3"

        String workflowNodeName = "formTrigger";
        String parameterPath = "inputs[0].c";
        Object value = 3;
        String type = "INTEGER";
        boolean includeInMetadata = false;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty inputsArrayProperty = mock(ArrayProperty.class);

        when(inputsArrayProperty.getName()).thenReturn("inputs");
        when(inputsArrayProperty.getDisplayCondition())
            .thenReturn(null);

        ObjectProperty inputObjectProperty = mock(ObjectProperty.class);

        when(inputObjectProperty.getName()).thenReturn(null);

        List<Property> inputObjectProperties = new ArrayList<>();

        // Property with triple compound condition
        Property tripleConditionProperty = mock(Property.class);

        when(tripleConditionProperty.getName()).thenReturn("result");
        when(tripleConditionProperty.getDisplayCondition())
            .thenReturn("inputs[index].a == 1 and inputs[index].b == 2 and inputs[index].c == 3");

        inputObjectProperties.add(tripleConditionProperty);

        when(inputObjectProperty.getProperties()).thenReturn((List) inputObjectProperties);
        when(inputsArrayProperty.getItems()).thenReturn((List) List.of(inputObjectProperty));

        properties.add(inputsArrayProperty);

        when(triggerDefinition.getProperties()).thenReturn((List) properties);
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> inputItem = new HashMap<>();

        inputItem.put("a", 1);
        inputItem.put("b", 2);
        inputItem.put("c", 0); // Will be updated to 3

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(inputItem);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("inputs", inputs);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "form/v1/newFormRequest");
        trigger.put("parameters", parameters);
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertNotNull(result.displayConditions());

        Map<String, Boolean> displayConditions = result.displayConditions();

        assertTrue(
            displayConditions.containsKey("inputs[0].a == 1 and inputs[0].b == 2 and inputs[0].c == 3"),
            "Triple compound condition should be in displayConditions. Actual: " + displayConditions);

        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterWithArrayPath() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "items[0].name";
        Object value = "arrayValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterWithComplexPath() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "nested.param[0].value";
        Object value = "complexValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

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

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterWithDependentProperties() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "connectionId";
        Object value = "new_connection";
        String type = "STRING";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = createMockActionDefinitionWithDependentProperties();

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

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

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterWithTriggerNode() {
        // Given
        String workflowNodeName = "triggerNode";
        String parameterPath = "param1";
        Object value = "triggerValue";
        String type = "STRING";
        boolean includeInMetadata = true;

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);

        Map<String, Object> trigger = new HashMap<>();

        trigger.put("name", workflowNodeName);
        trigger.put("type", "webhook/v1/webhook");
        trigger.put("parameters", new HashMap<>());
        trigger.put("metadata", new HashMap<>());

        List<Map<String, Object>> triggers = new ArrayList<>();

        triggers.add(trigger);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("triggers", triggers);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);
        verify(triggerDefinitionService).getTriggerDefinition(anyString(), anyInt(), anyString());
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterWithNullNestedObjectProperty() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "value.nestedproperty.childproperty";
        Object value = new HashMap<>(Map.of("innerKey", "innerValue"));
        String type = "OBJECT";
        boolean includeInMetadata = true;

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());

        Map<String, Object> nestedObjectValue = new HashMap<>();

        nestedObjectValue.put("nestedproperty", null);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("type", "OBJECT");
        parameters.put("value", nestedObjectValue);

        Map<String, Object> dynamicPropertyTypes = new HashMap<>();

        dynamicPropertyTypes.put("value.nestedproperty", "OBJECT");

        Map<String, Object> uiMetadata = new HashMap<>();

        uiMetadata.put("dynamicPropertyTypes", dynamicPropertyTypes);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("ui", uiMetadata);

        Map<String, Object> task = new HashMap<>();

        task.put("name", "task1");
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", metadata);

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, type, includeInMetadata, 0);

        // Then
        assertNotNull(result);

        Map<String, Object> persistedParameters = getPersistedParameters(workflowId, workflowNodeName);
        Map<String, Object> persistedNestedObjectValue = (Map<String, Object>) persistedParameters.get("value");

        Map<String, Object> resultNestedProperty =
            (Map<String, Object>) persistedNestedObjectValue.get("nestedproperty");

        assertNotNull(resultNestedProperty);
        assertEquals(value, resultNestedProperty.get("childproperty"));

        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterRemovesChildDynamicPropertyTypes() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "value[0]";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        List<Object> valueList = new ArrayList<>();
        Map<String, Object> valueItem = new HashMap<>();

        valueItem.put("arr", List.of(Map.of("arr1", List.of(Map.of("str 1", "hello")))));

        valueList.add(valueItem);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("type", "ARRAY");
        parameters.put("value", valueList);

        Map<String, Object> dynamicPropertyTypes = new HashMap<>();

        dynamicPropertyTypes.put("value[0].arr", "ARRAY");
        dynamicPropertyTypes.put("value[0].arr[0].arr1", "ARRAY");
        dynamicPropertyTypes.put("value[0].arr[0].arr1[0].str 1", "STRING");

        Map<String, Object> uiMetadata = new HashMap<>();

        uiMetadata.put("dynamicPropertyTypes", dynamicPropertyTypes);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("ui", uiMetadata);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", metadata);

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertTrue(getPersistedDynamicPropertyTypes(workflowId, workflowNodeName).isEmpty());
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterReindexesSiblingChildDynamicPropertyTypes() {
        // Given
        String workflowNodeName = "task1";
        String parameterPath = "value[0]";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        List<Object> valueList = new ArrayList<>();

        valueList.add(Map.of("field", "first"));
        valueList.add(Map.of("field", "second"));

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("type", "ARRAY");
        parameters.put("value", valueList);

        Map<String, Object> dynamicPropertyTypes = new HashMap<>();

        dynamicPropertyTypes.put("value[0].field", "STRING");
        dynamicPropertyTypes.put("value[1].field", "STRING");
        dynamicPropertyTypes.put("value[1].nested[0].sub", "NUMBER");

        Map<String, Object> uiMetadata = new HashMap<>();

        uiMetadata.put("dynamicPropertyTypes", dynamicPropertyTypes);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("ui", uiMetadata);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", metadata);

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);

        Map<String, Object> persistedDynamicPropertyTypes = getPersistedDynamicPropertyTypes(
            workflowId, workflowNodeName);

        assertEquals(2, persistedDynamicPropertyTypes.size());
        assertTrue(persistedDynamicPropertyTypes.containsKey("value[0].field"));
        assertTrue(persistedDynamicPropertyTypes.containsKey("value[0].nested[0].sub"));
        assertFalse(persistedDynamicPropertyTypes.containsKey("value[1].field"));
        assertFalse(persistedDynamicPropertyTypes.containsKey("value[1].nested[0].sub"));
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterRemovesEmptyParentMap() {
        // Given: "config" has only "value"; deleting "config.value" should remove "config" entirely
        String workflowNodeName = "task1";
        String parameterPath = "config.value";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> configMap = new HashMap<>();

        configMap.put("value", "hello");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("config", configMap);
        parameters.put("otherParam", "keep");

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertFalse(result.parameters()
            .containsKey("config"), "Empty parent map should be removed");
        assertTrue(result.parameters()
            .containsKey("otherParam"), "Non-empty parameters should be preserved");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterRemovesEmptyList() {
        // Given: "tags" has one item; deleting "tags[0]" should remove the empty "tags" list
        String workflowNodeName = "task1";
        String parameterPath = "tags[0]";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        List<Object> tagsList = new ArrayList<>();

        tagsList.add("tag1");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("tags", tagsList);
        parameters.put("name", "test");

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertFalse(result.parameters()
            .containsKey("tags"), "Empty list should be removed after last item deleted");
        assertTrue(result.parameters()
            .containsKey("name"), "Non-empty parameters should be preserved");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterRemovesNestedEmptyMaps() {
        // Given: "config.nested.value" is the only leaf; deleting it should remove entire chain
        String workflowNodeName = "task1";
        String parameterPath = "config.nested.value";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> nestedMap = new HashMap<>();

        nestedMap.put("value", "deep");

        Map<String, Object> configMap = new HashMap<>();

        configMap.put("nested", nestedMap);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("config", configMap);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertFalse(
            result.parameters()
                .containsKey("config"),
            "Entire nested chain should be removed when all leaves are deleted");
        assertTrue(result.parameters()
            .isEmpty(), "Parameters should be empty after all values removed");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testDeleteWorkflowNodeParameterRemovesEmptyMapInsideListElement() {
        // Given: "items" has one map element with only "value" key;
        // Deleting "items[0].value" should make items[0] empty, which should be removed from the list,
        // and then the empty list should be removed

        String workflowNodeName = "task1";
        String parameterPath = "items[0].value";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> itemMap = new HashMap<>();

        itemMap.put("value", "hello");

        List<Object> itemsList = new ArrayList<>();

        itemsList.add(itemMap);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("items", itemsList);
        parameters.put("otherParam", "keep");

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.deleteWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, 0);

        // Then
        assertNotNull(result);
        assertFalse(result.parameters()
            .containsKey("items"), "List should be removed after its only element becomes an empty map");
        assertTrue(result.parameters()
            .containsKey("otherParam"), "Non-empty parameters should be preserved");
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterRemovesEmptyMapsInsideLists() {
        // Given: parameters have a list containing a mix of empty and non-empty map elements;
        // removeEmptyCollections should recurse into lists and remove empty map elements

        String workflowNodeName = "task1";
        String parameterPath = "newParam";
        Object value = "newValue";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        List<Object> itemsList = new ArrayList<>();

        itemsList.add(new HashMap<>(Map.of("key", "value")));
        itemsList.add(new HashMap<>());
        itemsList.add(new HashMap<>(Map.of("name", "test")));

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("items", itemsList);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, "STRING", false, 0);

        // Then
        assertNotNull(result);

        Map<String, ?> resultParameters = result.parameters();

        assertTrue(resultParameters.containsKey("items"), "Non-empty list should be preserved");

        List<?> resultItems = (List<?>) resultParameters.get("items");

        assertEquals(2, resultItems.size(), "Empty map element should be removed from list");
        assertEquals("newValue", resultParameters.get("newParam"));
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateWorkflowNodeParameterRemovesNestedEmptyCollectionsInsideLists() {
        // Given: parameters have a list containing maps with nested empty collections;
        // removeEmptyCollections should recursively clean up nested empty structures within list elements

        String workflowNodeName = "task1";
        String parameterPath = "newParam";
        Object value = "newValue";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> mapWithEmptyNested = new HashMap<>();

        mapWithEmptyNested.put("nested", new HashMap<>());

        Map<String, Object> mapWithValue = new HashMap<>();

        mapWithValue.put("key", "value");

        List<Object> innerList = new ArrayList<>();

        innerList.add(new HashMap<>());

        List<Object> itemsList = new ArrayList<>();

        itemsList.add(mapWithEmptyNested);
        itemsList.add(mapWithValue);
        itemsList.add(innerList);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("items", itemsList);

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, "STRING", false, 0);

        // Then
        assertNotNull(result);

        Map<String, ?> resultParameters = result.parameters();

        assertTrue(resultParameters.containsKey("items"), "List should still exist with non-empty elements");

        List<?> resultItems = (List<?>) resultParameters.get("items");

        assertEquals(
            1, resultItems.size(),
            "Only the map with 'key'='value' should remain; empty nested map and empty inner list should be removed");

        Map<String, ?> remainingItem = (Map<String, ?>) resultItems.getFirst();

        assertEquals("value", remainingItem.get("key"));
        assertEquals("newValue", resultParameters.get("newParam"));
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @Test
    void testUpdateWorkflowNodeParameterPreservesNonEmptyCollections() {
        // Given: parameters have both empty and non-empty collections
        String workflowNodeName = "task1";
        String parameterPath = "newParam";
        Object value = "newValue";

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getProperties()).thenReturn(new ArrayList<>());
        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(actionDefinition);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("emptyMap", new HashMap<>());
        parameters.put("emptyList", new ArrayList<>());
        parameters.put("nonEmptyMap", new HashMap<>(Map.of("key", "value")));
        parameters.put("nonEmptyList", new ArrayList<>(List.of("item1")));

        Map<String, Object> task = new HashMap<>();

        task.put("name", workflowNodeName);
        task.put("type", "component/v1/action");
        task.put("parameters", parameters);
        task.put("metadata", new HashMap<>());

        List<Map<String, Object>> tasks = new ArrayList<>();

        tasks.add(task);

        Map<String, Object> definitionMap = new HashMap<>();

        definitionMap.put("tasks", tasks);

        String workflowId = createWorkflow(definitionMap);

        // When
        ParameterResultDTO result = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            workflowId, workflowNodeName, parameterPath, value, "STRING", false, 0);

        // Then
        assertNotNull(result);

        Map<String, ?> resultParameters = result.parameters();

        assertFalse(resultParameters.containsKey("emptyMap"), "Empty map should be removed");
        assertFalse(resultParameters.containsKey("emptyList"), "Empty list should be removed");
        assertTrue(resultParameters.containsKey("nonEmptyMap"), "Non-empty map should be preserved");
        assertTrue(resultParameters.containsKey("nonEmptyList"), "Non-empty list should be preserved");
        assertEquals("newValue", resultParameters.get("newParam"));
        assertPersisted(workflowId, workflowNodeName, result);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private ActionDefinition createMockActionDefinitionWithComplexProperties() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        List<Property> properties = new ArrayList<>();

        ArrayProperty arrayProperty = mock(ArrayProperty.class);

        when(arrayProperty.getName()).thenReturn("items");

        properties.add(arrayProperty);

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

        ObjectProperty objectProperty = mock(ObjectProperty.class);

        when(objectProperty.getName()).thenReturn("config");
        when(objectProperty.getRequired()).thenReturn(true);

        properties.add(objectProperty);

        ArrayProperty arrayProperty = mock(ArrayProperty.class);

        when(arrayProperty.getName()).thenReturn("items");
        when(arrayProperty.getRequired()).thenReturn(false);

        properties.add(arrayProperty);

        when(actionDefinition.getProperties()).thenReturn((List) properties);

        return actionDefinition;
    }

    private void assertPersisted(String workflowId, String workflowNodeName, ParameterResultDTO result) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> workflowNode = getPersistedNode(workflowId, workflowNodeName);

        assertThat(result.version()).isEqualTo(workflow.getVersion());
        assertThat(workflowNode.getOrDefault("parameters", Map.of())).isEqualTo(result.parameters());
        assertThat(workflowNode.getOrDefault("metadata", Map.of())).isEqualTo(result.metadata());
    }

    private static Map<String, Object> createClusterElementDefinitionMap() {
        Map<String, Object> clusterElementMap = new HashMap<>();

        clusterElementMap.put("name", "loopTask");
        clusterElementMap.put("type", "loop/v1/loop");
        clusterElementMap.put("parameters", new HashMap<>());
        clusterElementMap.put("metadata", new HashMap<>());

        Map<String, Object> task = new HashMap<>();

        task.put("name", "task1");
        task.put("type", "component/v1/action");
        task.put("parameters", new HashMap<>(Map.of("param1", "x")));
        task.put("metadata", new HashMap<>());
        task.put("clusterElements", new HashMap<>(Map.of("loop", clusterElementMap)));

        return Map.of("tasks", List.of(task));
    }

    private String createWorkflow(Map<String, ?> definitionMap) {
        Workflow workflow = workflowService.create(
            JsonUtils.write(definitionMap), Format.JSON, Workflow.SourceType.JDBC);

        return workflow.getId();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findNode(Object value, String workflowNodeName) {
        if (value instanceof Map<?, ?> map) {
            if (workflowNodeName.equals(map.get("name")) && map.containsKey("type")) {
                return (Map<String, Object>) map;
            }

            for (Object entryValue : map.values()) {
                Map<String, Object> workflowNode = findNode(entryValue, workflowNodeName);

                if (workflowNode != null) {
                    return workflowNode;
                }
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> workflowNode = findNode(item, workflowNodeName);

                if (workflowNode != null) {
                    return workflowNode;
                }
            }
        }

        return null;
    }

    private Map<String, Object> getPersistedNode(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> workflowNode = findNode(JsonUtils.readMap(workflow.getDefinition()), workflowNodeName);

        assertThat(workflowNode).isNotNull();

        return workflowNode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPersistedParameters(String workflowId, String workflowNodeName) {
        Map<String, Object> workflowNode = getPersistedNode(workflowId, workflowNodeName);

        return (Map<String, Object>) workflowNode.getOrDefault("parameters", Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPersistedDynamicPropertyTypes(String workflowId, String workflowNodeName) {
        Map<String, Object> workflowNode = getPersistedNode(workflowId, workflowNodeName);

        Map<String, Object> metadata = (Map<String, Object>) workflowNode.getOrDefault("metadata", Map.of());
        Map<String, Object> ui = (Map<String, Object>) metadata.getOrDefault("ui", Map.of());

        return (Map<String, Object>) ui.getOrDefault("dynamicPropertyTypes", Map.of());
    }
}
