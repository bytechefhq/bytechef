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

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskExecutionServiceTest {

    private static final long TASK_EXECUTION_ID = 12L;

    private final TaskExecutionRepository taskExecutionRepository = mock(TaskExecutionRepository.class);
    private final TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

    @Test
    void testCancelIfUnfinishedCancelsStartedTaskExecution() {
        TaskExecution taskExecution = stubTaskExecution(TaskExecution.Status.STARTED);

        assertThat(taskExecutionService.cancelIfUnfinished(TASK_EXECUTION_ID)).isTrue();

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(taskExecution.getEndDate()).isNotNull();

        verify(taskExecutionRepository).save(taskExecution);
        verify(taskExecutionRepository).unlockForUpdate(TASK_EXECUTION_ID);
    }

    @Test
    void testCancelIfUnfinishedLeavesCompletedTaskExecution() {
        TaskExecution taskExecution = stubTaskExecution(TaskExecution.Status.COMPLETED);

        assertThat(taskExecutionService.cancelIfUnfinished(TASK_EXECUTION_ID)).isFalse();

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);

        verify(taskExecutionRepository, never()).save(any());
        verify(taskExecutionRepository).unlockForUpdate(TASK_EXECUTION_ID);
    }

    @Test
    void testCompleteIfNotCancelledCompletesStartedTaskExecution() {
        TaskExecution taskExecution = stubTaskExecution(TaskExecution.Status.STARTED);

        assertThat(taskExecutionService.completeIfNotCancelled(TASK_EXECUTION_ID)).isTrue();

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);

        verify(taskExecutionRepository).save(taskExecution);
        verify(taskExecutionRepository).unlockForUpdate(TASK_EXECUTION_ID);
    }

    @Test
    void testCompleteIfNotCancelledLeavesCancelledTaskExecution() {
        TaskExecution taskExecution = stubTaskExecution(TaskExecution.Status.CANCELLED);

        assertThat(taskExecutionService.completeIfNotCancelled(TASK_EXECUTION_ID)).isFalse();

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);

        verify(taskExecutionRepository, never()).save(any());
        verify(taskExecutionRepository).unlockForUpdate(TASK_EXECUTION_ID);
    }

    @Test
    void testUpdateKeepsStartDateOfClaimedTaskExecution() {
        Instant startDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution claimedTaskExecution = stubTaskExecution(TaskExecution.Status.COMPLETED);

        claimedTaskExecution.setStartDate(startDate);

        TaskExecution completedTaskExecution = new TaskExecution();

        completedTaskExecution.setId(TASK_EXECUTION_ID);
        completedTaskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecutionService.update(completedTaskExecution);

        assertThat(completedTaskExecution.getStartDate()).isEqualTo(startDate);

        verify(taskExecutionRepository).save(completedTaskExecution);
    }

    private TaskExecution stubTaskExecution(TaskExecution.Status status) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(TASK_EXECUTION_ID);
        taskExecution.setStatus(status);

        when(taskExecutionRepository.findByIdForUpdate(TASK_EXECUTION_ID))
            .thenReturn(Optional.of(taskExecution));

        return taskExecution;
    }
}
