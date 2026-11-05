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

package com.bytechef.task.dispatcher.fork.join.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ForkJoinTaskCompletionHandlerTest {

    private static final long FORK_JOIN_TASK_EXECUTION_ID = 10L;
    private static final long JOB_ID = 5L;

    private final CounterService counterService = mock(CounterService.class);
    private final TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

    @Test
    @SuppressWarnings("unchecked")
    void testLastBranchCollectsEveryBranchOutputEvenWhenAnotherBranchWriteWasLost() {
        TaskExecution forkJoinTaskExecution = TaskExecution.builder()
            .id(FORK_JOIN_TASK_EXECUTION_ID)
            .jobId(JOB_ID)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        "name", "forkJoin_1", "type", "forkJoin/v1",
                        "parameters", Map.of(
                            ForkJoinTaskDispatcherConstants.BRANCHES,
                            List.of(
                                List.of(Map.of("name", "branchZero", "type", "var/v1/set")),
                                List.of(Map.of("name", "branchOne", "type", "var/v1/set")))))))
            .build();

        FileEntry branchZeroOutput = new FileEntry("branch0.json", "base64:///branch0.json");
        FileEntry branchOneOutput = new FileEntry("branch1.json", "base64:///branch1.json");

        TaskExecution branchZeroTaskExecution = branchTaskExecution(1L, "branchZero", 0, branchZeroOutput);
        TaskExecution branchOneTaskExecution = branchTaskExecution(2L, "branchOne", 1, branchOneOutput);

        when(taskExecutionService.update(any(TaskExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(taskExecutionService.getTaskExecution(FORK_JOIN_TASK_EXECUTION_ID)).thenReturn(forkJoinTaskExecution);
        when(taskExecutionService.getTaskExecutionForUpdate(FORK_JOIN_TASK_EXECUTION_ID))
            .thenReturn(forkJoinTaskExecution);
        when(taskExecutionService.getParentTaskExecutions(FORK_JOIN_TASK_EXECUTION_ID))
            .thenReturn(List.of(branchZeroTaskExecution, branchOneTaskExecution));
        when(counterService.decrement(FORK_JOIN_TASK_EXECUTION_ID)).thenReturn(0L);
        when(taskFileStorage.readContextValue(any())).thenReturn(Map.of());
        when(taskFileStorage.readTaskExecutionOutput(branchZeroOutput)).thenReturn("zero");
        when(taskFileStorage.readTaskExecutionOutput(branchOneOutput)).thenReturn("one");

        ForkJoinTaskCompletionHandler forkJoinTaskCompletionHandler = new ForkJoinTaskCompletionHandler(
            mock(ContextService.class), counterService, mock(Evaluator.class), taskExecutionService,
            taskCompletionHandler, task -> {}, taskFileStorage);

        forkJoinTaskCompletionHandler.handle(branchOneTaskExecution);

        ArgumentCaptor<Object> outputCaptor = ArgumentCaptor.forClass(Object.class);

        verify(taskFileStorage).storeTaskExecutionOutput(eq(JOB_ID), anyLong(), outputCaptor.capture());

        assertThat((Map<String, Object>) outputCaptor.getValue())
            .containsEntry("branch_0", "zero")
            .containsEntry("branch_1", "one");
        verify(taskCompletionHandler).handle(forkJoinTaskExecution);
    }

    private static TaskExecution branchTaskExecution(long id, String name, int branch, FileEntry output) {
        TaskExecution branchTaskExecution = TaskExecution.builder()
            .id(id)
            .jobId(JOB_ID)
            .parentId(FORK_JOIN_TASK_EXECUTION_ID)
            .status(TaskExecution.Status.COMPLETED)
            .taskNumber(1)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        "name", name, "type", "var/v1/set",
                        "parameters", Map.of(ForkJoinTaskDispatcherConstants.BRANCH, branch))))
            .build();

        branchTaskExecution.setOutput(output);

        return branchTaskExecution;
    }
}
