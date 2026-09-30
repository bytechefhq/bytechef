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

package com.bytechef.atlas.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskCoordinatorTest {

    private final TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    @SuppressWarnings("unchecked")
    private final TaskCoordinator taskCoordinator = new TaskCoordinator(
        List.of(), List.of(), mock(ApplicationEventPublisher.class), mock(JobExecutor.class), mock(JobService.class),
        taskCompletionHandler, (TaskDispatcher<? super Task>) mock(TaskDispatcher.class), taskExecutionService);

    @Test
    void testCompletionOfCancelledTaskExecutionIsDropped() {
        when(taskExecutionService.completeIfNotCancelled(12L))
            .thenReturn(false);

        taskCoordinator.onTaskExecutionCompleteEvent(
            new TaskExecutionCompleteEvent(createTaskExecution(TaskExecution.Status.STARTED)));

        verify(taskCompletionHandler, never()).handle(any());
    }

    @Test
    void testCompletionOfStartedTaskExecutionIsHandled() {
        TaskExecution completedTaskExecution = createTaskExecution(TaskExecution.Status.STARTED);

        when(taskExecutionService.completeIfNotCancelled(12L))
            .thenReturn(true);

        taskCoordinator.onTaskExecutionCompleteEvent(new TaskExecutionCompleteEvent(completedTaskExecution));

        verify(taskCompletionHandler).handle(completedTaskExecution);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResumeOfFailedJobDispatchesFailedTaskWithCompletedTaskOutputs() {
        Job job = new Job();

        job.setCurrentTask(1);
        job.setEndDate(Instant.now());
        job.setId(4567L);
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId("workflow1");

        JobRepository jobRepository = mock(JobRepository.class);

        when(jobRepository.findById(4567L))
            .thenReturn(Optional.of(job));

        WorkflowService workflowService = mock(WorkflowService.class);

        when(workflowService.getWorkflow("workflow1"))
            .thenReturn(
                new Workflow(
                    "workflow1",
                    """
                        {
                            "tasks": [
                                {"name": "task_1", "type": "noop/v1/noop"},
                                {"name": "task_2", "type": "noop/v1/noop", "parameters": {"value": "${task_1}"}},
                                {"name": "task_3", "type": "noop/v1/noop"}
                            ]
                        }
                        """,
                    Workflow.Format.JSON));

        TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

        when(taskFileStorage.readContextValue(any()))
            .thenReturn((Map) Map.of("task_1", "task 1 output"));

        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

        when(taskExecutionService.create(any()))
            .thenAnswer(invocation -> {
                TaskExecution taskExecution = invocation.getArgument(0);

                taskExecution.setId(13L);

                return taskExecution;
            });

        TaskDispatcher<? super Task> taskDispatcher = (TaskDispatcher<? super Task>) mock(TaskDispatcher.class);
        ContextService contextService = mock(ContextService.class);
        JobService jobService = new JobServiceImpl(jobRepository);

        TaskCoordinator resumingTaskCoordinator = new TaskCoordinator(
            List.of(), List.of(), mock(ApplicationEventPublisher.class),
            new JobExecutor(
                contextService, SpelEvaluator.create(), taskDispatcher, taskExecutionService, taskFileStorage,
                workflowService),
            jobService, taskCompletionHandler, taskDispatcher, taskExecutionService);

        resumingTaskCoordinator.onResumeJobEvent(new ResumeJobEvent(4567L));

        ArgumentCaptor<TaskExecution> taskExecutionArgumentCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher).dispatch(taskExecutionArgumentCaptor.capture());

        TaskExecution dispatchedTaskExecution = taskExecutionArgumentCaptor.getValue();

        assertThat(dispatchedTaskExecution.getName()).isEqualTo("task_2");
        Map<String, ?> parameters = dispatchedTaskExecution.getParameters();

        assertThat(parameters.get("value")).isEqualTo("task 1 output");
        assertThat(job.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(job.getEndDate()).isNull();
        assertThat(job.getCurrentTask()).isEqualTo(1);

        verify(contextService).peek(4567L, Context.Classname.JOB);
    }

    private static TaskExecution createTaskExecution(TaskExecution.Status status) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(12L);
        taskExecution.setJobId(4567L);
        taskExecution.setStatus(status);

        return taskExecution;
    }
}
