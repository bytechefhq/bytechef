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
import java.util.List;
import java.util.Map;
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

    @Test
    void countJobTaskExecutionsCountsOnlyLatestAttempts() {
        when(taskExecutionRepository.countLatestAttemptsByJobId(7L, List.of(3L))).thenReturn(4L);

        assertThat(taskExecutionService.countJobTaskExecutions(7L, List.of(3L))).isEqualTo(4L);
    }

    @Test
    void fetchLastStartedJobTaskExecutionReadsLastStartedRow() {
        TaskExecution startedTaskExecution = new TaskExecution();

        when(taskExecutionRepository.findLastStartedByJobId(7L)).thenReturn(Optional.of(startedTaskExecution));

        assertThat(taskExecutionService.fetchLastStartedJobTaskExecution(7L)).containsSame(startedTaskExecution);
    }

    @Test
    void getChildCountsReadsGroupedCountsPerParent() {
        when(taskExecutionRepository.countAllByParentIdIn(List.of(7L, 8L)))
            .thenReturn(Map.of(7L, 2L, 8L, 1L));

        assertThat(taskExecutionService.getChildCounts(List.of(7L, 8L))).isEqualTo(Map.of(7L, 2L, 8L, 1L));
    }

    @Test
    void getIterationCountsReadsGroupedIterationStartCountsPerParent() {
        when(taskExecutionRepository.countAllByParentIdInAndTaskNumber(List.of(7L, 8L), 0))
            .thenReturn(Map.of(7L, 1L, 8L, 2L));

        assertThat(taskExecutionService.getIterationCounts(List.of(7L, 8L))).isEqualTo(Map.of(7L, 1L, 8L, 2L));
    }

    @Test
    void getParentIterationStartTaskExecutionsReadsTaskNumberZeroRowsAtOffset() {
        TaskExecution iterationStartTaskExecution = new TaskExecution();

        when(taskExecutionRepository.findAllByParentIdAndTaskNumberOrderByCreatedDate(9L, 0, 51, 100L))
            .thenReturn(List.of(iterationStartTaskExecution));

        assertThat(taskExecutionService.getParentIterationStartTaskExecutions(9L, 100L, 51))
            .containsExactly(iterationStartTaskExecution);
    }

    @Test
    void getParentTaskExecutionsBetweenReadsHalfOpenRange() {
        Instant fromCreatedDate = Instant.parse("2026-09-25T10:00:00Z");
        Instant toCreatedDate = Instant.parse("2026-09-25T10:00:05Z");

        TaskExecution childTaskExecution = new TaskExecution();

        when(taskExecutionRepository.findAllByParentIdAndCreatedDateBetweenOrderByCreatedDate(
            9L, fromCreatedDate, 1L, toCreatedDate, 2L)).thenReturn(List.of(childTaskExecution));

        assertThat(
            taskExecutionService.getParentTaskExecutionsBetween(
                9L, createTaskExecution(1L, fromCreatedDate), createTaskExecution(2L, toCreatedDate)))
                    .containsExactly(childTaskExecution);
    }

    @Test
    void getParentTaskExecutionsFromReadsOpenEndedRange() {
        Instant fromCreatedDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution childTaskExecution = new TaskExecution();

        when(taskExecutionRepository.findAllByParentIdAndCreatedDateFromOrderByCreatedDate(9L, fromCreatedDate, 1L))
            .thenReturn(List.of(childTaskExecution));

        assertThat(taskExecutionService.getParentTaskExecutionsFrom(9L, createTaskExecution(1L, fromCreatedDate)))
            .containsExactly(childTaskExecution);
    }

    @Test
    void getTaskExecutionsKeepsRequestedOrder() {
        TaskExecution first = new TaskExecution();
        TaskExecution second = new TaskExecution();

        first.setId(1L);
        second.setId(2L);

        when(taskExecutionRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(second, first));

        assertThat(taskExecutionService.getTaskExecutions(List.of(1L, 2L))).containsExactly(first, second);
    }

    @Test
    void getTaskExecutionsSkipsIdsThatAreNotFound() {
        TaskExecution second = new TaskExecution();

        second.setId(2L);

        when(taskExecutionRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(second));

        assertThat(taskExecutionService.getTaskExecutions(List.of(1L, 2L))).containsExactly(second);
    }

    @Test
    void getTaskExecutionsQueriesEachIdOnce() {
        TaskExecution first = new TaskExecution();

        first.setId(1L);

        when(taskExecutionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(first));

        assertThat(taskExecutionService.getTaskExecutions(List.of(1L, 1L))).containsExactly(first, first);
    }

    @Test
    void getIterationIndexIsZeroBasedCount() {
        Instant createdDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getId()).thenReturn(TASK_EXECUTION_ID);
        when(taskExecution.getParentId()).thenReturn(5L);
        when(taskExecution.getCreatedDate()).thenReturn(createdDate);
        when(taskExecutionRepository.countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
            5L, 0, createdDate, TASK_EXECUTION_ID)).thenReturn(3L);

        assertThat(taskExecutionService.getIterationIndex(taskExecution)).isEqualTo(2);
    }

    @Test
    void getIterationIndexCountsTheLeadingIteration() {
        Instant createdDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getId()).thenReturn(TASK_EXECUTION_ID);
        when(taskExecution.getParentId()).thenReturn(5L);
        when(taskExecution.getCreatedDate()).thenReturn(createdDate);
        when(taskExecutionRepository.countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
            5L, 0, createdDate, TASK_EXECUTION_ID)).thenReturn(0L);
        when(taskExecutionRepository.findAllIdsByIdInAndFirstChildTaskNumberNot(List.of(5L), 0))
            .thenReturn(List.of(5L));

        assertThat(taskExecutionService.getIterationIndex(taskExecution)).isZero();
    }

    @Test
    void getIterationCountsAddTheLeadingIteration() {
        when(taskExecutionRepository.countAllByParentIdInAndTaskNumber(List.of(7L, 8L, 9L), 0))
            .thenReturn(Map.of(7L, 1L, 8L, 2L));
        when(taskExecutionRepository.findAllIdsByIdInAndFirstChildTaskNumberNot(List.of(7L, 8L, 9L), 0))
            .thenReturn(List.of(8L, 9L));

        assertThat(taskExecutionService.getIterationCounts(List.of(7L, 8L, 9L)))
            .isEqualTo(Map.of(7L, 1L, 8L, 3L, 9L, 1L));
    }

    @Test
    void getChildIndexIsZeroBasedPosition() {
        Instant createdDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getId()).thenReturn(TASK_EXECUTION_ID);
        when(taskExecution.getParentId()).thenReturn(5L);
        when(taskExecution.getCreatedDate()).thenReturn(createdDate);
        when(taskExecutionRepository.countByParentIdAndCreatedDateBeforeOrEqual(5L, createdDate, TASK_EXECUTION_ID))
            .thenReturn(74L);

        assertThat(taskExecutionService.getChildIndex(taskExecution)).isEqualTo(73);
    }

    @Test
    void fetchTaskExecutionIsEmptyForMissingRow() {
        when(taskExecutionRepository.findById(TASK_EXECUTION_ID))
            .thenReturn(Optional.empty());

        assertThat(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).isEmpty();
    }

    @Test
    void getJobTopLevelTaskExecutionsReadsOnlyTopLevelRows() {
        TaskExecution taskExecution = new TaskExecution();

        when(taskExecutionRepository.findAllByJobIdAndParentIdIsNullOrderByCreatedDate(7L))
            .thenReturn(List.of(taskExecution));

        assertThat(taskExecutionService.getJobTopLevelTaskExecutions(7L)).containsExactly(taskExecution);

        verify(taskExecutionRepository, never()).findAllByJobIdOrderByCreatedDate(7L);
    }

    @Test
    void getParentTaskExecutionsUsesPageOffset() {
        taskExecutionService.getParentTaskExecutions(9L, 2, 50);

        verify(taskExecutionRepository).findAllByParentIdOrderByCreatedDate(9L, 50, 100L);
    }

    private static TaskExecution createTaskExecution(long id, Instant createdDate) {
        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getId()).thenReturn(id);
        when(taskExecution.getCreatedDate()).thenReturn(createdDate);

        return taskExecution;
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
