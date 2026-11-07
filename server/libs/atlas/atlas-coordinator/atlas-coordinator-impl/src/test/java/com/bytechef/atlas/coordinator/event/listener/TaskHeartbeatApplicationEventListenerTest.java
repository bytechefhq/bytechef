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

package com.bytechef.atlas.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.TaskHeartbeatApplicationEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskHeartbeatApplicationEventListenerTest {

    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskHeartbeatApplicationEventListener taskHeartbeatApplicationEventListener =
        new TaskHeartbeatApplicationEventListener(taskExecutionService);

    @Test
    void testHeartbeatTouchesAStartedTaskExecution() {
        TaskExecution taskExecution = createTaskExecution(7L, TaskExecution.Status.STARTED);

        when(taskExecutionService.getTaskExecution(7L)).thenReturn(taskExecution);

        taskHeartbeatApplicationEventListener.onApplicationEvent(new TaskHeartbeatApplicationEvent(7L));

        verify(taskExecutionService).update(taskExecution);
    }

    @Test
    void testHeartbeatLeavesATerminatedTaskExecutionUntouched() {
        for (TaskExecution.Status status : List.of(
            TaskExecution.Status.COMPLETED, TaskExecution.Status.FAILED, TaskExecution.Status.CANCELLED)) {

            when(taskExecutionService.getTaskExecution(8L)).thenReturn(createTaskExecution(8L, status));

            taskHeartbeatApplicationEventListener.onApplicationEvent(new TaskHeartbeatApplicationEvent(8L));
        }

        verify(taskExecutionService, never()).update(any());
    }

    @Test
    void testHeartbeatFailureIsSwallowed() {
        when(taskExecutionService.getTaskExecution(9L)).thenThrow(new IllegalStateException("database down"));

        assertThatCode(
            () -> taskHeartbeatApplicationEventListener.onApplicationEvent(new TaskHeartbeatApplicationEvent(9L)))
                .doesNotThrowAnyException();
    }

    private static TaskExecution createTaskExecution(long id, TaskExecution.Status status) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setStatus(status);

        return taskExecution;
    }
}
