
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class TaskExecutionErrorHandlerTest {

    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);

    @Test
    public void test1() {
        when(jobService.getTaskExecutionJob("1234")).thenReturn(new Job("4567"));

        TaskExecutionErrorHandler taskExecutionErrorHandler = new TaskExecutionErrorHandler(eventPublisher, jobService,
            taskDispatcher, taskExecutionService);

        TaskExecution erroredTaskExecution = new TaskExecution();

        erroredTaskExecution.setId("1234");
        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));

        taskExecutionErrorHandler.handle(erroredTaskExecution);
        taskExecutionErrorHandler.handle(erroredTaskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test2() {
        when(jobService.getTaskExecutionJob("1234")).thenReturn(new Job());

        TaskExecutionErrorHandler taskExecutionErrorHandler = new TaskExecutionErrorHandler(eventPublisher, jobService,
            taskDispatcher, taskExecutionService);

        TaskExecution erroredTaskExecution = new TaskExecution();

        erroredTaskExecution.setId("1234");
        erroredTaskExecution.setError(new ExecutionError("something bad happened", List.of()));
        erroredTaskExecution.setRetry(1);

        taskExecutionErrorHandler.handle(erroredTaskExecution);

        verify(taskDispatcher, times(1)).dispatch(any());
    }
}
