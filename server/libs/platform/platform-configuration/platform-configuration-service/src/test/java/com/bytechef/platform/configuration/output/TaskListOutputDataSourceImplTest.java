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

package com.bytechef.platform.configuration.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class TaskListOutputDataSourceImplTest {

    private static final String WORKFLOW_ID = "workflow1";
    private static final String LAST_TASK_NAME = "action1";
    private static final long ENVIRONMENT_ID = 1L;

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    private TaskListOutputDataSourceImpl taskListOutputDataSource;

    @BeforeEach
    void setUp() {
        taskListOutputDataSource = new TaskListOutputDataSourceImpl(
            actionDefinitionService, taskDispatcherDefinitionService, workflowNodeOutputFacade,
            workflowNodeTestOutputService);
    }

    @Test
    void testGetLastTaskOutputReturnsTestOutputSampleWhenPresent() {
        WorkflowNodeTestOutput workflowNodeTestOutput = mock(WorkflowNodeTestOutput.class);

        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowNodeTestOutput));
        when(workflowNodeTestOutput.getSampleOutput()).thenReturn(Map.of("key", "value"));

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            WORKFLOW_ID, LAST_TASK_NAME, "component/v1/action1", ENVIRONMENT_ID);

        assertEquals(Map.of("key", "value"), outputResponse.getSampleOutput());

        verifyNoInteractions(actionDefinitionService, taskDispatcherDefinitionService, workflowNodeOutputFacade);
    }

    @Test
    void testGetLastTaskOutputReturnsStaticActionOutputWhenNoTestOutput() {
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse())
            .thenReturn(new com.bytechef.platform.domain.OutputResponse(null, Map.of("key", "value"), null));

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            WORKFLOW_ID, LAST_TASK_NAME, "component/v1/action1", ENVIRONMENT_ID);

        assertEquals(Map.of("key", "value"), outputResponse.getSampleOutput());

        verifyNoInteractions(taskDispatcherDefinitionService, workflowNodeOutputFacade);
    }

    @Test
    void testGetLastTaskOutputReturnsStaticDispatcherOutputForTaskDispatcherType() {
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("map", 1))
            .thenReturn(taskDispatcherDefinition);
        when(taskDispatcherDefinition.getOutputResponse())
            .thenReturn(new com.bytechef.platform.domain.OutputResponse(null, Map.of("key", "value"), null));

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            WORKFLOW_ID, LAST_TASK_NAME, "map/v1", ENVIRONMENT_ID);

        assertEquals(Map.of("key", "value"), outputResponse.getSampleOutput());

        verifyNoInteractions(actionDefinitionService, workflowNodeOutputFacade);
    }

    @Test
    void testGetLastTaskOutputFallsBackToLiveComputationWhenNoStaticOutput() {
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse()).thenReturn(null);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = mock(WorkflowNodeOutputDTO.class);

        when(workflowNodeOutputFacade.getWorkflowNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(workflowNodeOutputDTO);
        when(workflowNodeOutputDTO.getSampleOutput()).thenReturn(Map.of("key", "value"));

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            WORKFLOW_ID, LAST_TASK_NAME, "component/v1/action1", ENVIRONMENT_ID);

        assertEquals(Map.of("key", "value"), outputResponse.getSampleOutput());
    }

    @Test
    void testGetLastTaskOutputReturnsNullWhenNothingAvailable() {
        when(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse()).thenReturn(null);
        when(workflowNodeOutputFacade.getWorkflowNodeOutput(WORKFLOW_ID, LAST_TASK_NAME, ENVIRONMENT_ID))
            .thenReturn(null);

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            WORKFLOW_ID, LAST_TASK_NAME, "component/v1/action1", ENVIRONMENT_ID);

        assertNull(outputResponse);
    }

    @Test
    void testGetLastTaskOutputSkipsWorkflowScopedLookupsWhenWorkflowIdIsNull() {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("component", 1, "action1"))
            .thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse())
            .thenReturn(new com.bytechef.platform.domain.OutputResponse(null, Map.of("key", "value"), null));

        OutputResponse outputResponse = taskListOutputDataSource.getLastTaskOutput(
            null, LAST_TASK_NAME, "component/v1/action1", ENVIRONMENT_ID);

        assertEquals(Map.of("key", "value"), outputResponse.getSampleOutput());

        verifyNoInteractions(workflowNodeTestOutputService, workflowNodeOutputFacade);
    }
}
