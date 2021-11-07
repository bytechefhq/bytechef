/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integri.atlas.engine.coordinator.job.SimpleJob;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class TaskExecutionErrorHandlerTest {

    private JobRepository jobRepo = mock(JobRepository.class);
    private TaskExecutionRepository taskRepo = mock(TaskExecutionRepository.class);
    private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
    private EventPublisher eventPublisher = mock(EventPublisher.class);

    @Test
    public void test1() {
        when(jobRepo.getByTaskId("1234")).thenReturn(new SimpleJob(Collections.singletonMap("id", "4567")));
        TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
        handler.setEventPublisher(eventPublisher);
        handler.setJobRepository(jobRepo);
        handler.setJobTaskRepository(taskRepo);
        SimpleTaskExecution errorable = new SimpleTaskExecution();
        errorable.setId("1234");
        errorable.setError(new ErrorObject("something bad happened", new String[0]));
        handler.handle(errorable);
        handler.handle(errorable);
        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test2() {
        when(jobRepo.getByTaskId("1234")).thenReturn(new SimpleJob());
        TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
        handler.setJobRepository(jobRepo);
        handler.setJobTaskRepository(taskRepo);
        handler.setTaskDispatcher(taskDispatcher);
        SimpleTaskExecution errorable = SimpleTaskExecution.of("retry", 1);
        errorable.setId("1234");
        errorable.setError(new ErrorObject("something bad happened", new String[0]));
        handler.handle(errorable);
        verify(taskDispatcher, times(1)).dispatch(any());
    }
}
