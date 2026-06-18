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

package com.bytechef.atlas.coordinator.task.completion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.TaskExecution;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskCompletionHandlerChainTest {

    @Test
    void testHandleStopsAfterTaskIsHandled() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        TaskCompletionHandler consumingHandler = mock(TaskCompletionHandler.class);

        when(consumingHandler.canHandle(taskExecution))
            .thenReturn(true);

        doAnswer(invocation -> {
            taskExecution.setHandled(true);

            return null;
        }).when(consumingHandler)
            .handle(taskExecution);

        TaskCompletionHandler subsequentHandler = mock(TaskCompletionHandler.class);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(List.of(consumingHandler, subsequentHandler));

        taskCompletionHandlerChain.handle(taskExecution);

        verify(consumingHandler).handle(taskExecution);
        verify(subsequentHandler, never()).canHandle(any(TaskExecution.class));
        verify(subsequentHandler, never()).handle(any(TaskExecution.class));
    }

    @Test
    void testHandleRunsSubsequentHandlerWhenTaskNotHandled() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        TaskCompletionHandler firstHandler = mock(TaskCompletionHandler.class);

        when(firstHandler.canHandle(taskExecution))
            .thenReturn(false);

        TaskCompletionHandler secondHandler = mock(TaskCompletionHandler.class);

        when(secondHandler.canHandle(taskExecution))
            .thenReturn(true);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(List.of(firstHandler, secondHandler));

        taskCompletionHandlerChain.handle(taskExecution);

        verify(secondHandler).handle(taskExecution);
    }
}
