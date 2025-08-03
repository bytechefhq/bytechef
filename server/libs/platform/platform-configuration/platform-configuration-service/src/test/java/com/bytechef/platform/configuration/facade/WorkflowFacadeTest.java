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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowFacadeTest {

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private WorkflowService workflowService;

    private WorkflowFacade workflowFacade;

    @Mock
    private Workflow testWorkflow;

    @BeforeEach
    public void beforeEach() {
        workflowFacade = new WorkflowFacadeImpl(componentConnectionFacade, componentDefinitionService, workflowService);

        when(testWorkflow.getId()).thenReturn("test-workflow-id");
        when(testWorkflow.getDefinition())
            .thenReturn("{\"label\": \"Test Workflow\", \"description\": \"Test Description\", \"tasks\": []}");
        when(testWorkflow.getLabel()).thenReturn("Test Workflow");
        when(testWorkflow.getDescription()).thenReturn("Test Description");
        when(testWorkflow.getFormat()).thenReturn(Workflow.Format.JSON);
        when(testWorkflow.getInputs()).thenReturn(Collections.emptyList());
        when(testWorkflow.getOutputs()).thenReturn(Collections.emptyList());
        when(testWorkflow.getSourceType()).thenReturn(Workflow.SourceType.JDBC);
        when(testWorkflow.getMaxRetries()).thenReturn(0);
        when(testWorkflow.getVersion()).thenReturn(1);

        when(workflowService.getWorkflow(anyString())).thenReturn(testWorkflow);
    }

    @Test
    public void testGetWorkflow() {
        when(testWorkflow.getTasks(true)).thenReturn(Collections.emptyList());

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(testWorkflow.getId());

        assertThat(workflowDTO).isNotNull();
        assertThat(workflowDTO.getLabel()).isEqualTo("Test Workflow");
        assertThat(workflowDTO.getDescription()).isEqualTo("Test Description");
        assertThat(workflowDTO.getTasks()).isEmpty();
        assertThat(workflowDTO.getTriggers()).isEmpty();
    }

    @Test
    public void testGetWorkflowWithClusterRoot() {
        WorkflowTask mockTask = mock(WorkflowTask.class);

        when(mockTask.getName()).thenReturn("testTask");
        when(mockTask.getType()).thenReturn("testComponent/v1/testOperation");
        when(mockTask.getExtensions()).thenReturn(Collections.emptyMap());

        when(testWorkflow.getTasks(true)).thenReturn(List.of(mockTask));

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(Collections.emptyList());

        ComponentDefinition mockComponentDefinition = mock(ComponentDefinition.class);

        when(mockComponentDefinition.isClusterRoot()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition(anyString(), anyInt()))
            .thenReturn(Optional.of(mockComponentDefinition));

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(testWorkflow.getId());

        verify(componentDefinitionService).fetchComponentDefinition(eq("testComponent"), eq(1));

        assertThat(workflowDTO).isNotNull();

        List<WorkflowTaskDTO> tasks = workflowDTO.getTasks();

        assertThat(tasks).hasSize(1);

        WorkflowTaskDTO taskDTO = tasks.getFirst();

        assertThat(taskDTO).isNotNull();
        assertThat(taskDTO.getName()).isEqualTo("testTask");
        assertThat(taskDTO.isClusterRoot()).isTrue();
    }
}
