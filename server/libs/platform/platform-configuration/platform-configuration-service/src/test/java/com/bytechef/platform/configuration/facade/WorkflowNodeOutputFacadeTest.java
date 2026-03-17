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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
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
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeOutputFacadeTest {

    private static final String WORKFLOW_ID = "workflow1";
    private static final long ENVIRONMENT_ID = 1L;

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ActionDefinitionService actionDefinitionService;

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
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowCacheManager workflowCacheManager;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeOutputFacadeImpl workflowNodeOutputFacade;

    @BeforeEach
    void setUp() {
        workflowNodeOutputFacade = new WorkflowNodeOutputFacadeImpl(
            actionDefinitionFacade, actionDefinitionService, clusterElementDefinitionFacade,
            clusterElementDefinitionService, evaluator, taskDispatcherDefinitionService, triggerDefinitionFacade,
            triggerDefinitionService, workflowCacheManager, workflowService, workflowNodeTestOutputService,
            workflowTestConfigurationService);
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsReturnsOutputsForPreviousNodes() {
        WorkflowTask task1 = new WorkflowTask(
            Map.of("name", "action1", "type", "component/v1/action1"));
        WorkflowTask task2 = new WorkflowTask(
            Map.of("name", "action2", "type", "component/v1/action2"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(eq("action2"))).thenReturn(List.of(task1, task2));

        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        OutputResponse outputResponse = mock(OutputResponse.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, "action1", ENVIRONMENT_ID))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse()).thenReturn(outputResponse);

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            List<WorkflowNodeOutputDTO> result = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                WORKFLOW_ID, "action2", ENVIRONMENT_ID);

            assertEquals(1, result.size());
            assertEquals("action1", result.getFirst()
                .workflowNodeName());
        }
    }

    @Test
    void testGetPreviousWorkflowNodeSampleOutputsReturnsSampleOutputs() {
        WorkflowTask task1 = new WorkflowTask(
            Map.of("name", "action1", "type", "component/v1/action1"));
        WorkflowTask task2 = new WorkflowTask(
            Map.of("name", "action2", "type", "component/v1/action2"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(eq("action2"))).thenReturn(List.of(task1, task2));

        ActionDefinition actionDefinition = mock(ActionDefinition.class);
        OutputResponse outputResponse = new OutputResponse(null, Map.of("key", "value"), null);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, "action1", ENVIRONMENT_ID))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse()).thenReturn(outputResponse);

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            Map<String, ?> result = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                WORKFLOW_ID, "action2", ENVIRONMENT_ID);

            assertEquals(1, result.size());
            assertTrue(result.containsKey("action1"));
        }
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsIncludesLoopTaskDispatcherOutputForSiblings() {
        WorkflowTask loopTask = new WorkflowTask(
            Map.of("name", "loop1", "type", "loop/v1", "parameters", Map.of("iteratee", List.of())));
        WorkflowTask task2 = new WorkflowTask(
            Map.of("name", "action2", "type", "component/v1/action2"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(eq("action2"))).thenReturn(List.of(loopTask, task2));

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);
        OutputResponse loopOutputResponse = mock(OutputResponse.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, "loop1", ENVIRONMENT_ID))
            .thenReturn(Optional.empty());
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(taskDispatcherDefinition);
        when(taskDispatcherDefinitionService.isDynamicOutputDefined("loop", 1)).thenReturn(false);
        when(taskDispatcherDefinition.getOutputResponse()).thenReturn(loopOutputResponse);

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            List<WorkflowNodeOutputDTO> result = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                WORKFLOW_ID, "action2", ENVIRONMENT_ID);

            assertEquals(1, result.size());
            assertEquals("loop1", result.getFirst()
                .workflowNodeName());
            assertNotNull(result.getFirst()
                .taskDispatcherDefinition());
        }
    }

    @Test
    void testSampleOutputsCachePreventsDuplicateComputation() {
        WorkflowTask task1 = new WorkflowTask(
            Map.of("name", "action1", "type", "component/v1/action1"));
        WorkflowTask task2 = new WorkflowTask(
            Map.of("name", "action2", "type", "component/v1/action2"));
        WorkflowTask task3 = new WorkflowTask(
            Map.of("name", "action3", "type", "component/v1/action3"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        // For the top-level call: getPreviousWorkflowNodeOutputs("action3")
        when(workflow.getTasks(eq("action3"))).thenReturn(List.of(task1, task2, task3));

        // For the recursive call: doGetPreviousWorkflowNodeSampleOutputs("action2")
        when(workflow.getTasks(eq("action2"))).thenReturn(List.of(task1, task2));

        // action1 has a static output
        ActionDefinition action1Definition = mock(ActionDefinition.class);
        OutputResponse action1Output = new OutputResponse(null, Map.of("field1", "value1"), null);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(eq(WORKFLOW_ID), eq("action1"), anyLong()))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(action1Definition);
        when(action1Definition.getOutputResponse()).thenReturn(action1Output);

        // action2 has a dynamic output that requires fetching previous sample outputs
        ActionDefinition action2Definition = mock(ActionDefinition.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(eq(WORKFLOW_ID), eq("action2"), anyLong()))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action2"))
            .thenReturn(action2Definition);
        when(action2Definition.getOutputResponse()).thenReturn(null);
        when(actionDefinitionService.isDynamicOutputDefined("component", 1, "action2")).thenReturn(true);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Map.of());
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            WORKFLOW_ID, "action2", ENVIRONMENT_ID))
                .thenReturn(List.of());

        when(evaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OutputResponse dynamicOutput = new OutputResponse(null, Map.of("dynamic", "result"), null);

        when(actionDefinitionFacade.executeOutput(
            eq("component"), eq(1), eq("action2"), any(), any()))
                .thenReturn(dynamicOutput);

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            List<WorkflowNodeOutputDTO> result = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                WORKFLOW_ID, "action3", ENVIRONMENT_ID);

            assertEquals(2, result.size());
            assertEquals("action1", result.get(0)
                .workflowNodeName());
            assertEquals("action2", result.get(1)
                .workflowNodeName());
        }

        // action1's definition should be fetched exactly twice: once for the top-level iteration over action1,
        // and once for the recursive call when computing action2's dynamic output (which needs action1's output).
        // Without the cache, the recursive call from action2 would trigger yet another fetch of action1, but the
        // cache deduplicates at the doGetPreviousWorkflowNodeSampleOutputs level.
        verify(actionDefinitionService, times(2)).getActionDefinition("component", 1, "action1");
    }

    @Test
    void testSampleOutputsCacheDeduplicatesAcrossMultipleDynamicNodes() {
        WorkflowTask task1 = new WorkflowTask(
            Map.of("name", "action1", "type", "component/v1/action1"));
        WorkflowTask task2 = new WorkflowTask(
            Map.of("name", "action2", "type", "component/v1/action2"));
        WorkflowTask task3 = new WorkflowTask(
            Map.of("name", "action3", "type", "component/v1/action3"));
        WorkflowTask task4 = new WorkflowTask(
            Map.of("name", "action4", "type", "component/v1/action4"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        // Top-level: getPreviousWorkflowNodeOutputs("action4")
        when(workflow.getTasks(eq("action4"))).thenReturn(List.of(task1, task2, task3, task4));

        // Recursive calls for dynamic output computation
        when(workflow.getTasks(eq("action2"))).thenReturn(List.of(task1, task2));
        when(workflow.getTasks(eq("action3"))).thenReturn(List.of(task1, task2, task3));

        // action1: static output
        ActionDefinition action1Definition = mock(ActionDefinition.class);
        OutputResponse action1Output = new OutputResponse(null, Map.of("f1", "v1"), null);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(eq(WORKFLOW_ID), eq("action1"), anyLong()))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(action1Definition);
        when(action1Definition.getOutputResponse()).thenReturn(action1Output);

        // action2: dynamic output
        ActionDefinition action2Definition = mock(ActionDefinition.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(eq(WORKFLOW_ID), eq("action2"), anyLong()))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action2"))
            .thenReturn(action2Definition);
        when(action2Definition.getOutputResponse()).thenReturn(null);
        when(actionDefinitionService.isDynamicOutputDefined("component", 1, "action2")).thenReturn(true);

        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Map.of());
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
            eq(WORKFLOW_ID), anyString(), eq(ENVIRONMENT_ID)))
                .thenReturn(List.of());

        when(evaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OutputResponse dynamic2Output = new OutputResponse(null, Map.of("d2", "r2"), null);

        when(actionDefinitionFacade.executeOutput(
            eq("component"), eq(1), eq("action2"), any(), any()))
                .thenReturn(dynamic2Output);

        // action3: also dynamic output — its recursive call needs outputs for action1 and action2,
        // which should be served from the cache (action2's sample outputs were already computed)
        ActionDefinition action3Definition = mock(ActionDefinition.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(eq(WORKFLOW_ID), eq("action3"), anyLong()))
            .thenReturn(Optional.empty());
        when(actionDefinitionService.getActionDefinition("component", 1, "action3"))
            .thenReturn(action3Definition);
        when(action3Definition.getOutputResponse()).thenReturn(null);
        when(actionDefinitionService.isDynamicOutputDefined("component", 1, "action3")).thenReturn(true);

        OutputResponse dynamic3Output = new OutputResponse(null, Map.of("d3", "r3"), null);

        when(actionDefinitionFacade.executeOutput(
            eq("component"), eq(1), eq("action3"), any(), any()))
                .thenReturn(dynamic3Output);

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            List<WorkflowNodeOutputDTO> result = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                WORKFLOW_ID, "action4", ENVIRONMENT_ID);

            assertEquals(3, result.size());
        }

        // The cache prevents re-traversal of predecessor chains. Without it,
        // doGetPreviousWorkflowNodeSampleOutputs("action2") would call
        // doGetPreviousWorkflowNodeOutputs("action2") → workflow.getTasks("action2")
        // multiple times: once from action2's own dynamic output computation, and
        // again from action3's recursive computation that also encounters action2.
        //
        // With the cache, workflow.getTasks("action2") is called only once because
        // the second doGetPreviousWorkflowNodeSampleOutputs("action2") call (from
        // inside action3's recursive processing) returns the cached result.
        verify(workflow, times(1)).getTasks(eq("action2"));
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsReturnsEmptyListForFirstNode() {
        WorkflowTask task1 = new WorkflowTask(
            Map.of("name", "action1", "type", "component/v1/action1"));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(eq("action1"))).thenReturn(List.of(task1));

        try (MockedStatic<WorkflowTrigger> workflowTriggerStatic = mockStatic(WorkflowTrigger.class)) {
            workflowTriggerStatic.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            List<WorkflowNodeOutputDTO> result = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                WORKFLOW_ID, "action1", ENVIRONMENT_ID);

            assertTrue(result.isEmpty());
        }
    }
}
