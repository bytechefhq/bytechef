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

package com.bytechef.atlas.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinatorIntTest;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    properties = {
        "bytechef.workflow.repository.classpath.enabled=true"
    })
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
class StoppedJobTaskDispatcherIntTest {

    private final List<Task> dispatchedTasks = new ArrayList<>();

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    private TaskDispatcherChain taskDispatcherChain;

    @BeforeEach
    void beforeEach() {
        dispatchedTasks.clear();

        taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            List.of(new StoppedJobTaskDispatcher(jobService, taskExecutionService), new RecordingTaskDispatcher()));
    }

    @Test
    void testDispatchCancelsTaskOfJobStoppedByUser() {
        Job job = createStartedJob();

        jobService.setStatusToStopped(getId(job));

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.CREATED);

        taskDispatcherChain.dispatch(taskExecution);

        TaskExecution persistedTaskExecution = taskExecutionService.getTaskExecution(getId(taskExecution));

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(persistedTaskExecution.getEndDate()).isNotNull();
        assertThat(dispatchedTasks).isEmpty();
    }

    @Test
    void testDispatchCancelsStartedFlowControlTaskOfJobStoppedByUser() {
        Job job = createStartedJob();

        jobService.setStatusToStopped(getId(job));

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.STARTED);

        taskDispatcherChain.dispatch(taskExecution);

        TaskExecution persistedTaskExecution = taskExecutionService.getTaskExecution(getId(taskExecution));

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(dispatchedTasks).isEmpty();
    }

    @Test
    void testDispatchPassesTaskOfSuspendedJob() {
        Job job = createStartedJob();

        job.setMetadata(Map.of(SuspendedJobs.JOB_RESUME_ID, "resume-id"));

        jobService.update(job);

        jobService.setStatusToStopped(getId(job));

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.CREATED);

        taskDispatcherChain.dispatch(taskExecution);

        TaskExecution persistedTaskExecution = taskExecutionService.getTaskExecution(getId(taskExecution));

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CREATED);
        assertThat(dispatchedTasks).containsExactly(taskExecution);
    }

    @Test
    void testDispatchPassesTaskOfRunningJob() {
        Job job = createStartedJob();

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.CREATED);

        taskDispatcherChain.dispatch(taskExecution);

        assertThat(dispatchedTasks).containsExactly(taskExecution);
    }

    @Test
    void testDispatchPassesTaskAfterStoppedJobResumes() {
        Job job = createStartedJob();

        jobService.setStatusToStopped(getId(job));

        jobService.resumeToStatusStarted(getId(job));

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.CREATED);

        taskDispatcherChain.dispatch(taskExecution);

        assertThat(dispatchedTasks).containsExactly(taskExecution);
    }

    @Test
    void testDispatchPassesControlTask() {
        Job job = createStartedJob();

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.STARTED);

        jobService.setStatusToStopped(getId(job));

        CancelControlTask cancelControlTask = new CancelControlTask(getId(job), getId(taskExecution));

        taskDispatcherChain.dispatch(cancelControlTask);

        assertThat(dispatchedTasks).containsExactly(cancelControlTask);
    }

    @Test
    void testDispatchPassesTaskOfDeletedJob() {
        Job job = createStartedJob();

        TaskExecution taskExecution = createTaskExecution(getId(job), TaskExecution.Status.CREATED);

        taskExecution.setJobId(Long.MAX_VALUE);

        taskDispatcherChain.dispatch(taskExecution);

        assertThat(dispatchedTasks).containsExactly(taskExecution);
    }

    private Job createStartedJob() {
        Job job = jobService.create(
            new JobParametersDTO("aGVsbG8x", Map.of("yourName", "me")), workflowService.getWorkflow("aGVsbG8x"));

        return jobService.setStatusToStarted(getId(job));
    }

    private TaskExecution createTaskExecution(long jobId, TaskExecution.Status status) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .startDate(status == TaskExecution.Status.STARTED ? Instant.now() : null)
            .status(status)
            .taskNumber(0)
            .workflowTask(new WorkflowTask(Map.of("name", "task0", "type", "test/v1")))
            .build();

        return taskExecutionService.create(taskExecution);
    }

    private static long getId(Job job) {
        return Objects.requireNonNull(job.getId());
    }

    private static long getId(TaskExecution taskExecution) {
        return Objects.requireNonNull(taskExecution.getId());
    }

    private class RecordingTaskDispatcher implements TaskDispatcher<Task>, TaskDispatcherResolver {

        @Override
        public void dispatch(Task task) {
            dispatchedTasks.add(task);
        }

        @Override
        public TaskDispatcher<? extends Task> resolve(Task task) {
            return this;
        }
    }
}
