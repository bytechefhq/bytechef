/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.error.ExecutionError;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Arik Cohen
 */
public class TaskExecutionErrorEventListenerTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ContextService contextService = mock(ContextService.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    public void test1() {
        when(jobService.getTaskExecutionJob(1234L))
            .thenReturn(new Job(4567L));

        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            eventPublisher, contextService, jobService, taskDispatcher, taskExecutionService, taskFileStorage);

        TaskExecution erroredTaskExecution = new TaskExecution();

        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));
        erroredTaskExecution.setId(1234L);

        when(taskExecutionService.update(any()))
            .thenReturn(erroredTaskExecution);

        taskExecutionErrorEventListener.onErrorEvent(new TaskExecutionErrorEvent(erroredTaskExecution));
        taskExecutionErrorEventListener.onErrorEvent(new TaskExecutionErrorEvent(erroredTaskExecution));

        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test2() {
        when(jobService.getTaskExecutionJob(1234L))
            .thenReturn(new Job());

        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            eventPublisher, contextService, jobService, taskDispatcher, taskExecutionService, taskFileStorage);

        TaskExecution erroredTaskExecution = new TaskExecution();

        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));
        erroredTaskExecution.setId(1234L);
        erroredTaskExecution.setJobId(4321L);
        erroredTaskExecution.setMaxRetries(1);
        erroredTaskExecution.setWorkflowTask(
            new WorkflowTask(
                Map.of(
                    "name", "workflowTaskName",
                    "type", "workflowTaskType")));

        when(taskExecutionService.update(any(TaskExecution.class))).thenReturn(erroredTaskExecution);
        when(taskExecutionService.create(any(TaskExecution.class))).thenReturn(erroredTaskExecution);

        taskExecutionErrorEventListener.onErrorEvent(new TaskExecutionErrorEvent(erroredTaskExecution));

        verify(taskDispatcher, times(1)).dispatch(any());
    }

    @Test
    public void testFailingJobCancelsUnfinishedSiblingTaskExecutions() {
        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            eventPublisher, contextService, jobService, taskDispatcher, taskExecutionService, taskFileStorage);

        TaskExecution forkJoinTaskExecution =
            createTaskExecution(10L, null, "fork-join/v1", TaskExecution.Status.STARTED);
        TaskExecution erroredTaskExecution = createTaskExecution(
            11L, 10L, "firecrawl/v1/scrape", TaskExecution.Status.STARTED);

        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));

        TaskExecution startedSiblingTaskExecution = createTaskExecution(
            12L, 10L, "firecrawl/v1/scrape", TaskExecution.Status.STARTED);
        TaskExecution createdSiblingTaskExecution = createTaskExecution(
            13L, 10L, "openAi/v1/ask", TaskExecution.Status.CREATED);
        TaskExecution completedSiblingTaskExecution = createTaskExecution(
            14L, 10L, "script/v1/javascript", TaskExecution.Status.COMPLETED);

        when(taskExecutionService.update(any(TaskExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(taskExecutionService.getTaskExecution(10L))
            .thenReturn(forkJoinTaskExecution);
        when(jobService.getTaskExecutionJob(10L))
            .thenReturn(new Job(4567L));
        when(taskExecutionService.getJobTaskExecutions(4567L))
            .thenReturn(
                List.of(
                    forkJoinTaskExecution, erroredTaskExecution, startedSiblingTaskExecution,
                    createdSiblingTaskExecution, completedSiblingTaskExecution));

        taskExecutionErrorEventListener.onErrorEvent(new TaskExecutionErrorEvent(erroredTaskExecution));

        assertThat(startedSiblingTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(startedSiblingTaskExecution.getEndDate()).isNotNull();
        assertThat(createdSiblingTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(completedSiblingTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
        assertThat(erroredTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(forkJoinTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);

        verify(taskDispatcher, times(1)).dispatch(isA(CancelControlTask.class));

        InOrder inOrder = inOrder(taskExecutionService, eventPublisher);

        inOrder.verify(taskExecutionService)
            .update(createdSiblingTaskExecution);
        inOrder.verify(eventPublisher)
            .publishEvent(isA(JobStatusApplicationEvent.class));
    }

    private static TaskExecution createTaskExecution(
        long id, @Nullable Long parentId, String type, TaskExecution.Status status) {

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setJobId(4567L);
        taskExecution.setParentId(parentId);
        taskExecution.setStatus(status);
        taskExecution.setWorkflowTask(new WorkflowTask(Map.of("name", "task" + id, "type", type)));

        return taskExecution;
    }
}
