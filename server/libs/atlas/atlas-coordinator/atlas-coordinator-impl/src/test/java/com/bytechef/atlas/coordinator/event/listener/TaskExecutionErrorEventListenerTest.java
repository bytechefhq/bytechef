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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.coordinator.event.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 */
public class TaskExecutionErrorEventListenerTest {

    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    public void test1() {
        when(jobService.getTaskExecutionJob(1234L))
            .thenReturn(new Job(4567L));

        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            eventPublisher, jobService, taskDispatcher, taskExecutionService);

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
            eventPublisher, jobService, taskDispatcher, taskExecutionService);

        TaskExecution erroredTaskExecution = new TaskExecution();

        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));
        erroredTaskExecution.setId(1234L);
        erroredTaskExecution.setMaxRetries(1);

        when(taskExecutionService.update(any()))
            .thenReturn(erroredTaskExecution);

        taskExecutionErrorEventListener.onErrorEvent(new TaskExecutionErrorEvent(erroredTaskExecution));

        verify(taskDispatcher, times(1)).dispatch(any());
    }
}
