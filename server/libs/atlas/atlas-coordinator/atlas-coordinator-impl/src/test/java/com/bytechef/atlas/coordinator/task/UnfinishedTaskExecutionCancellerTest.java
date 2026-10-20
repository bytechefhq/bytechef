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

package com.bytechef.atlas.coordinator.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class UnfinishedTaskExecutionCancellerTest {

    private static final long JOB_ID = 4567L;

    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final UnfinishedTaskExecutionCanceller unfinishedTaskExecutionCanceller =
        new UnfinishedTaskExecutionCanceller(taskDispatcher, taskExecutionService);

    @Test
    void testCancelSendsNoCancelControlTaskWhenTheJobHasNoTaskExecutions() {
        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(List.of());

        unfinishedTaskExecutionCanceller.cancel(JOB_ID);

        verify(taskExecutionService, never()).cancelIfUnfinished(anyLong());
        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testCancelSkipsTerminatedTaskExecutions() {
        when(taskExecutionService.getJobTaskExecutions(JOB_ID))
            .thenReturn(
                List.of(
                    createTaskExecution(1L, TaskExecution.Status.COMPLETED),
                    createTaskExecution(2L, TaskExecution.Status.FAILED),
                    createTaskExecution(3L, TaskExecution.Status.CANCELLED)));

        unfinishedTaskExecutionCanceller.cancel(JOB_ID);

        verify(taskExecutionService, never()).cancelIfUnfinished(anyLong());
        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testCancelSendsNoCancelControlTaskWhenNoTaskExecutionWasCancelled() {
        when(taskExecutionService.getJobTaskExecutions(JOB_ID))
            .thenReturn(
                List.of(
                    createTaskExecution(1L, TaskExecution.Status.STARTED),
                    createTaskExecution(2L, TaskExecution.Status.CREATED)));
        when(taskExecutionService.cancelIfUnfinished(anyLong())).thenReturn(false);

        unfinishedTaskExecutionCanceller.cancel(JOB_ID);

        verify(taskExecutionService).cancelIfUnfinished(1L);
        verify(taskExecutionService).cancelIfUnfinished(2L);
        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    void testCancelKeepsCancellingAndSendsCancelControlTaskWhenOneCancelFails() {
        when(taskExecutionService.getJobTaskExecutions(JOB_ID))
            .thenReturn(
                List.of(
                    createTaskExecution(1L, TaskExecution.Status.STARTED),
                    createTaskExecution(2L, TaskExecution.Status.STARTED)));
        when(taskExecutionService.cancelIfUnfinished(1L))
            .thenThrow(new IllegalStateException("TaskExecution with id '1' not found"));
        when(taskExecutionService.cancelIfUnfinished(2L)).thenReturn(true);

        unfinishedTaskExecutionCanceller.cancel(JOB_ID);

        verify(taskExecutionService).cancelIfUnfinished(2L);
        verify(taskDispatcher).dispatch(isA(CancelControlTask.class));
    }

    private static TaskExecution createTaskExecution(long id, TaskExecution.Status status) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setJobId(JOB_ID);
        taskExecution.setStatus(status);

        return taskExecution;
    }
}
