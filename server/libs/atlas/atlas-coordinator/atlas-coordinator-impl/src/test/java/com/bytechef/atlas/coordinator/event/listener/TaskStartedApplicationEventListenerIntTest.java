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

package com.bytechef.atlas.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.TaskCoordinatorIntTest;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = TaskCoordinatorIntTest.CoordinatorIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.classpath.enabled=true")
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
class TaskStartedApplicationEventListenerIntTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);

    private TaskStartedApplicationEventListener taskStartedApplicationEventListener;

    @BeforeEach
    void beforeEach() {
        taskStartedApplicationEventListener = new TaskStartedApplicationEventListener(
            taskExecutionService, taskDispatcher, jobService);
    }

    @Test
    void testTaskOfStartedJobIsMarkedStarted() {
        TaskExecution taskExecution = startTask(TaskExecution.Status.CREATED, Job.Status.STARTED, Map.of());

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);
        assertThat(taskExecution.getStartDate()).isNotNull();

        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testTaskOfSuspendedJobKeepsRunning() {
        TaskExecution taskExecution = startTask(
            TaskExecution.Status.CREATED, Job.Status.STOPPED, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);

        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testTaskOfStoppedJobIsCancelled() {
        TaskExecution taskExecution = startTask(TaskExecution.Status.CREATED, Job.Status.STOPPED, Map.of());

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.CREATED);

        verify(taskDispatcher).dispatch(isA(CancelControlTask.class));
    }

    @Test
    void testCancelledTaskOfSuspendedJobIsCancelled() {
        startTask(
            TaskExecution.Status.CANCELLED, Job.Status.STOPPED, Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        verify(taskDispatcher).dispatch(isA(CancelControlTask.class));
    }

    private TaskExecution startTask(
        TaskExecution.Status taskExecutionStatus, Job.Status jobStatus, Map<String, Object> jobMetadata) {

        Job job = new Job();

        job.setMetadata(jobMetadata);
        job.setStatus(jobStatus);
        job.setWorkflowId("aGVsbG8x");

        job = jobRepository.save(job);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(Objects.requireNonNull(job.getId()))
            .status(taskExecutionStatus)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "noop/v1/noop")))
            .build();

        taskExecution = taskExecutionRepository.save(taskExecution);

        long taskExecutionId = Objects.requireNonNull(taskExecution.getId());

        taskStartedApplicationEventListener.onApplicationEvent(new TaskStartedApplicationEvent(taskExecutionId));

        return taskExecutionRepository.findById(taskExecutionId)
            .orElseThrow();
    }
}
