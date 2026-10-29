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

package com.bytechef.task.dispatcher.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
class WaitForApprovalTaskDispatcherIntTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDispatchMarksTheStoppedJobAsWaitingForApproval() {
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        Job job = new Job();

        job.setMetadata(Map.of("projectVersion", 2));
        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("approvalWorkflow");

        job = jobService.update(job);

        long jobId = Objects.requireNonNull(job.getId());

        TaskExecution taskExecution = taskExecutionService.create(
            TaskExecution.builder()
                .jobId(jobId)
                .taskNumber(0)
                .workflowTask(new WorkflowTask(Map.of("name", "approval_1", "type", "waitForApproval/v1")))
                .build());

        WaitForApprovalTaskDispatcher waitForApprovalTaskDispatcher = new WaitForApprovalTaskDispatcher(
            event -> {}, jobService, taskExecutionService);

        waitForApprovalTaskDispatcher.dispatch(taskExecution);

        Job stoppedJob = jobService.getJob(jobId);

        assertThat(stoppedJob.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(stoppedJob.getMetadata(SuspendedJobs.WAIT_FOR_APPROVAL)).isEqualTo(true);
        assertThat(stoppedJob.getMetadata("projectVersion")).isEqualTo(2);
        assertThat(SuspendedJobs.isStoppedByUser(stoppedJob)).isFalse();
    }
}
