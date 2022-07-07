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

import com.bytechef.atlas.error.ErrorObject;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.domain.SimpleJob;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class TaskExecutionErrorHandlerTest {

    private JobService jobService = mock(JobService.class);
    private TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
    private EventPublisher eventPublisher = mock(EventPublisher.class);

    @Test
    public void test1() {
        when(jobService.getTaskExecutionJob("1234")).thenReturn(new SimpleJob(Collections.singletonMap("id", "4567")));
        TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
        handler.setEventPublisher(eventPublisher);
        handler.setJobService(jobService);
        handler.setTaskExecutionService(taskExecutionService);
        SimpleTaskExecution errorable = new SimpleTaskExecution();
        errorable.setId("1234");
        errorable.setError(new ErrorObject("something bad happened", new String[0]));
        handler.handle(errorable);
        handler.handle(errorable);
        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test2() {
        when(jobService.getTaskExecutionJob("1234")).thenReturn(new SimpleJob());
        TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
        handler.setJobService(jobService);
        handler.setTaskExecutionService(taskExecutionService);
        handler.setTaskDispatcher(taskDispatcher);
        SimpleTaskExecution errorable = SimpleTaskExecution.of("retry", 1);
        errorable.setId("1234");
        errorable.setError(new ErrorObject("something bad happened", new String[0]));
        handler.handle(errorable);
        verify(taskDispatcher, times(1)).dispatch(any());
    }
}
