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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class TaskCoordinatorTest {

    private final TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    @SuppressWarnings("unchecked")
    private final TaskCoordinator taskCoordinator = new TaskCoordinator(
        List.of(), List.of(), mock(ApplicationEventPublisher.class), mock(JobExecutor.class), mock(JobService.class),
        taskCompletionHandler, (TaskDispatcher<? super Task>) mock(TaskDispatcher.class), taskExecutionService);

    @Test
    void testCompletionOfCancelledTaskExecutionIsDropped() {
        when(taskExecutionService.getTaskExecution(12L))
            .thenReturn(createTaskExecution(TaskExecution.Status.CANCELLED));

        taskCoordinator.onTaskExecutionCompleteEvent(
            new TaskExecutionCompleteEvent(createTaskExecution(TaskExecution.Status.STARTED)));

        verify(taskCompletionHandler, never()).handle(any());
    }

    @Test
    void testCompletionOfStartedTaskExecutionIsHandled() {
        TaskExecution completedTaskExecution = createTaskExecution(TaskExecution.Status.STARTED);

        when(taskExecutionService.getTaskExecution(12L))
            .thenReturn(createTaskExecution(TaskExecution.Status.STARTED));

        taskCoordinator.onTaskExecutionCompleteEvent(new TaskExecutionCompleteEvent(completedTaskExecution));

        verify(taskCompletionHandler).handle(completedTaskExecution);
    }

    private static TaskExecution createTaskExecution(TaskExecution.Status status) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(12L);
        taskExecution.setJobId(4567L);
        taskExecution.setStatus(status);

        return taskExecution;
    }
}
