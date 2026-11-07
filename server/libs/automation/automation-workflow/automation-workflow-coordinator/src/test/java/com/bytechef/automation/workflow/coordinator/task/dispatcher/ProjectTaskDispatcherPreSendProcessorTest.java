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

package com.bytechef.automation.workflow.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
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
class ProjectTaskDispatcherPreSendProcessorTest {

    private static final long JOB_ID = 1L;
    private static final long PROJECT_DEPLOYMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 3L;
    private static final String WORKFLOW_ID = "workflow1";
    private static final String TASK_NAME = "action1";

    @Mock
    private JobService jobService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    private ProjectTaskDispatcherPreSendProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ProjectTaskDispatcherPreSendProcessor(
            jobService, projectDeploymentWorkflowService, principalJobService, jobPrincipalAccessorRegistry);
    }

    @Test
    void processCopiesDryRunFlagFromJobMetadata() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(JOB_ID);
        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        doReturn(Map.of(MetadataConstants.DRY_RUN, true)).when(job)
            .getMetadata();
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(principalJobService.getJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
            PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, TASK_NAME)).thenReturn(List.of());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.getId()).thenReturn(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(projectDeploymentWorkflow);
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            PlatformType.AUTOMATION)).thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getEnvironmentId(anyLong())).thenReturn(1L);

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.DRY_RUN)).isEqualTo(true);
    }

    @Test
    void processDefaultsDryRunToFalseWhenAbsentFromJobMetadata() {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(TASK_NAME);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(JOB_ID);
        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        doReturn(Map.of()).when(job)
            .getMetadata();
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(principalJobService.getJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
            PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, TASK_NAME)).thenReturn(List.of());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.getId()).thenReturn(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(projectDeploymentWorkflow);
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            PlatformType.AUTOMATION)).thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getEnvironmentId(anyLong())).thenReturn(1L);

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.DRY_RUN)).isEqualTo(false);
    }
}
