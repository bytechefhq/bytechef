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

package com.bytechef.task.dispatcher.subflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
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
class SubflowTaskDispatcherTest {

    private static final String INPUTS_NAME = "trigger_1";
    private static final String RESOLVED_WORKFLOW_ID = "resolved-workflow-id";
    private static final String WORKFLOW_UUID = "test-workflow-uuid";

    @Mock
    private JobService jobService;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private SubflowResolver subflowResolver;

    private SubflowTaskDispatcher subflowTaskDispatcher;

    @BeforeEach
    void setUp() {
        subflowTaskDispatcher = new SubflowTaskDispatcher(jobService, principalJobFacade, subflowResolver);
    }

    @Test
    void testDispatchResolvesWorkflowIdInNonEditorMode() {
        TaskExecution taskExecution = createTaskExecution(
            Map.of("workflowUuid", WORKFLOW_UUID), "subflow/v1");

        taskExecution.setJobId(1L);
        taskExecution.setId(10L);

        Job job = new Job();

        job.setId(1L);
        job.setMetadata(Map.of());

        when(jobService.getJob(1L)).thenReturn(job);
        when(subflowResolver.resolveSubflow(WORKFLOW_UUID, false))
            .thenReturn(new Subflow(RESOLVED_WORKFLOW_ID, INPUTS_NAME));

        subflowTaskDispatcher.dispatch(taskExecution);

        verify(subflowResolver).resolveSubflow(WORKFLOW_UUID, false);
        verify(principalJobFacade).createChildJob(anyLong(), any(JobParametersDTO.class));
    }

    @Test
    void testDispatchResolvesWorkflowIdInEditorMode() {
        TaskExecution taskExecution = createTaskExecution(
            Map.of("workflowUuid", WORKFLOW_UUID), "subflow/v1");

        taskExecution.setJobId(1L);
        taskExecution.setId(10L);

        Job job = new Job();

        job.setId(1L);
        job.setMetadata(Map.of(MetadataConstants.EDITOR_ENVIRONMENT, true));

        when(jobService.getJob(1L)).thenReturn(job);
        when(subflowResolver.resolveSubflow(WORKFLOW_UUID, true))
            .thenReturn(new Subflow(RESOLVED_WORKFLOW_ID, INPUTS_NAME));

        subflowTaskDispatcher.dispatch(taskExecution);

        verify(subflowResolver).resolveSubflow(WORKFLOW_UUID, true);
        verify(principalJobFacade).createChildJob(anyLong(), any(JobParametersDTO.class));
    }

    @Test
    void testDispatchThrowsWhenResolvedWorkflowIdIsEmpty() {
        TaskExecution taskExecution = createTaskExecution(
            Map.of("workflowUuid", WORKFLOW_UUID), "subflow/v1");

        taskExecution.setJobId(1L);
        taskExecution.setId(10L);

        Job job = new Job();

        job.setId(1L);
        job.setMetadata(Map.of());

        when(jobService.getJob(1L)).thenReturn(job);
        when(subflowResolver.resolveSubflow(WORKFLOW_UUID, false)).thenReturn(new Subflow("", INPUTS_NAME));

        assertThrows(IllegalStateException.class, () -> subflowTaskDispatcher.dispatch(taskExecution));
    }

    @Test
    void testSubflowRecordRejectsNullWorkflowId() {
        assertThrows(NullPointerException.class, () -> new Subflow(null, INPUTS_NAME));
    }

    @Test
    void testSubflowRecordRejectsNullInputsName() {
        assertThrows(NullPointerException.class, () -> new Subflow(RESOLVED_WORKFLOW_ID, null));
    }

    @Test
    void testResolveReturnsThisForSubflowType() {
        TaskExecution taskExecution = createTaskExecution(Map.of(), "subflow/v1");

        org.junit.jupiter.api.Assertions.assertEquals(
            subflowTaskDispatcher, subflowTaskDispatcher.resolve(taskExecution));
    }

    @Test
    void testResolveReturnsNullForOtherType() {
        TaskExecution taskExecution = createTaskExecution(Map.of(), "other/v1");

        org.junit.jupiter.api.Assertions.assertNull(subflowTaskDispatcher.resolve(taskExecution));
    }

    private static TaskExecution createTaskExecution(Map<String, ?> parameters, String type) {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(Map.of("name", "testTask", "type", type, "parameters", parameters)))
            .build();

        return taskExecution;
    }
}
