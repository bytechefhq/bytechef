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

package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobExecutionErrorsTest {

    private static final long JOB_ID = 42L;

    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    @Nested
    class CheckForError {

        @Test
        void testReportsTheFailedTaskWhenALaterTaskExecutionDidNotFail() {
            TaskExecution failedTaskExecution = createTaskExecution(
                1L, TaskExecution.Status.FAILED, "Branch task failed");
            TaskExecution completedTaskExecution = createTaskExecution(2L, TaskExecution.Status.COMPLETED, null);

            when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
                .thenReturn(Optional.of(completedTaskExecution));
            when(taskExecutionService.getJobTaskExecutions(JOB_ID))
                .thenReturn(List.of(failedTaskExecution, completedTaskExecution));

            Job job = createJob(Job.Status.FAILED, "Job failed");

            assertThatThrownBy(() -> JobExecutionErrors.checkForError(job, taskExecutionService))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("Branch task failed")
                .extracting(throwable -> ((ExecutionException) throwable).getEntityClass())
                .isEqualTo(TaskExecution.class);
        }

        @Test
        void testReportsAnEarlierFailedTaskWhenTheLastFailedTaskHasNoErrorDetails() {
            TaskExecution failedTaskExecution = createTaskExecution(
                1L, TaskExecution.Status.FAILED, "Earlier task failed");
            TaskExecution failedTaskExecutionWithoutError = createTaskExecution(
                2L, TaskExecution.Status.FAILED, null);

            when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
                .thenReturn(Optional.of(failedTaskExecutionWithoutError));
            when(taskExecutionService.getJobTaskExecutions(JOB_ID))
                .thenReturn(List.of(failedTaskExecution, failedTaskExecutionWithoutError));

            Job job = createJob(Job.Status.FAILED, null);

            assertThatThrownBy(() -> JobExecutionErrors.checkForError(job, taskExecutionService))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("Earlier task failed");
        }

        @Test
        void testReportsTheLastFailedTaskError() {
            TaskExecution failedTaskExecution = createTaskExecution(
                1L, TaskExecution.Status.FAILED, "Last task failed");

            when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
                .thenReturn(Optional.of(failedTaskExecution));

            Job job = createJob(Job.Status.FAILED, "Job failed");

            assertThatThrownBy(() -> JobExecutionErrors.checkForError(job, taskExecutionService))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("Last task failed");
        }

        @Test
        void testFallsBackToTheJobErrorWhenNoTaskExecutionFailed() {
            TaskExecution completedTaskExecution = createTaskExecution(1L, TaskExecution.Status.COMPLETED, null);

            when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
                .thenReturn(Optional.of(completedTaskExecution));
            when(taskExecutionService.getJobTaskExecutions(JOB_ID))
                .thenReturn(List.of(completedTaskExecution));

            Job job = createJob(Job.Status.FAILED, "Job failed");

            assertThatThrownBy(() -> JobExecutionErrors.checkForError(job, taskExecutionService))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("Job failed")
                .extracting(throwable -> ((ExecutionException) throwable).getEntityClass())
                .isEqualTo(Job.class);
        }

        @Test
        void testDoesNotThrowForACompletedJob() {
            TaskExecution completedTaskExecution = createTaskExecution(1L, TaskExecution.Status.COMPLETED, null);

            when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
                .thenReturn(Optional.of(completedTaskExecution));

            Job job = createJob(Job.Status.COMPLETED, null);

            assertThatCode(() -> JobExecutionErrors.checkForError(job, taskExecutionService))
                .doesNotThrowAnyException();
        }
    }

    private static Job createJob(Job.Status status, @Nullable String errorMessage) {
        Job job = new Job();

        job.setId(JOB_ID);
        job.setStatus(status);

        if (errorMessage != null) {
            job.setError(new ExecutionError(errorMessage, List.of()));
        }

        return job;
    }

    private static TaskExecution createTaskExecution(
        long id, TaskExecution.Status status, @Nullable String errorMessage) {

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setStatus(status);

        if (errorMessage != null) {
            taskExecution.setError(new ExecutionError(errorMessage, List.of()));
        }

        return taskExecution;
    }
}
