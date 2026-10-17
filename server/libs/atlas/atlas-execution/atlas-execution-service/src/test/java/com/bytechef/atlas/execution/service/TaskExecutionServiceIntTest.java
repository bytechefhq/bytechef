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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.config.AtlasExecutionServiceIntTestConfiguration;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.error.ExecutionError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AtlasExecutionServiceIntTestConfiguration.class)
public class TaskExecutionServiceIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void testCancelIfUnfinishedCancelsStartedTaskExecution() {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.STARTED));

        assertThat(taskExecutionService.cancelIfUnfinished(taskExecutionId)).isTrue();

        TaskExecution persistedTaskExecution = getPersistedTaskExecution(taskExecutionId);

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(persistedTaskExecution.getEndDate()).isNotNull();

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCancelIfUnfinishedLeavesCompletedTaskExecution() {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.COMPLETED));

        TaskExecution taskExecution = getPersistedTaskExecution(taskExecutionId);

        assertThat(taskExecutionService.cancelIfUnfinished(taskExecutionId)).isFalse();

        TaskExecution persistedTaskExecution = getPersistedTaskExecution(taskExecutionId);

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
        assertThat(persistedTaskExecution.getEndDate()).isNull();
        assertThat(persistedTaskExecution.getLastModifiedDate()).isEqualTo(taskExecution.getLastModifiedDate());

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCancelIfUnfinishedWaitsForConcurrentRowLockAndSeesItsCommit() throws Exception {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.STARTED));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch lockedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<?> lockHolderFuture = executorService.submit(() -> transactionTemplate.executeWithoutResult(
                transactionStatus -> {
                    jdbcTemplate.queryForObject(
                        "SELECT id FROM task_execution WHERE id = ? FOR UPDATE", Long.class, taskExecutionId);
                    jdbcTemplate.update(
                        "UPDATE task_execution SET status = ? WHERE id = ?", TaskExecution.Status.COMPLETED.ordinal(),
                        taskExecutionId);

                    lockedLatch.countDown();

                    awaitLatch(releaseLatch);
                }));

            assertThat(lockedLatch.await(30, TimeUnit.SECONDS)).isTrue();

            Future<Boolean> cancelFuture = executorService.submit(
                () -> taskExecutionService.cancelIfUnfinished(taskExecutionId));

            assertThatThrownBy(() -> cancelFuture.get(1, TimeUnit.SECONDS))
                .as("cancelIfUnfinished must block on the row lock held by the concurrent transaction")
                .isInstanceOf(TimeoutException.class);

            releaseLatch.countDown();

            lockHolderFuture.get(30, TimeUnit.SECONDS);

            assertThat(cancelFuture.get(30, TimeUnit.SECONDS))
                .as("cancelIfUnfinished must see the status committed by the transaction it waited for")
                .isFalse();
        }

        assertThat(getPersistedTaskExecution(taskExecutionId).getStatus())
            .isEqualTo(TaskExecution.Status.COMPLETED);

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCompleteIfNotCancelledCompletesStartedTaskExecution() {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.STARTED));

        assertThat(taskExecutionService.completeIfNotCancelled(taskExecutionId)).isTrue();

        assertThat(getPersistedTaskExecution(taskExecutionId).getStatus())
            .isEqualTo(TaskExecution.Status.COMPLETED);

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCompleteIfNotCancelledLeavesCancelledTaskExecution() {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.CANCELLED));

        TaskExecution taskExecution = getPersistedTaskExecution(taskExecutionId);

        assertThat(taskExecutionService.completeIfNotCancelled(taskExecutionId)).isFalse();

        TaskExecution persistedTaskExecution = getPersistedTaskExecution(taskExecutionId);

        assertThat(persistedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(persistedTaskExecution.getLastModifiedDate()).isEqualTo(taskExecution.getLastModifiedDate());

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCompleteIfNotCancelledCompletesAlreadyCompletedTaskExecution() {
        long taskExecutionId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.COMPLETED));

        assertThat(taskExecutionService.completeIfNotCancelled(taskExecutionId)).isTrue();

        assertThat(getPersistedTaskExecution(taskExecutionId).getStatus())
            .isEqualTo(TaskExecution.Status.COMPLETED);

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testUpdateKeepsStartDateOfClaimedTaskExecution() {
        Instant startDate = Instant.parse("2026-09-25T10:00:00Z");

        TaskExecution claimedTaskExecution = TaskExecution.builder()
            .jobId(saveJob())
            .startDate(startDate)
            .status(TaskExecution.Status.COMPLETED)
            .taskNumber(1)
            .workflowTask(newWorkflowTask("task1"))
            .build();

        long taskExecutionId = getId(taskExecutionRepository.save(claimedTaskExecution));

        TaskExecution completedTaskExecution = getPersistedTaskExecution(taskExecutionId);

        completedTaskExecution.setStartDate(null);
        completedTaskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecutionService.update(completedTaskExecution);

        assertThat(completedTaskExecution.getStartDate()).isEqualTo(startDate);
        assertThat(getPersistedTaskExecution(taskExecutionId).getStartDate()).isEqualTo(startDate);

        assertRowLockReleased(taskExecutionId);
    }

    @Test
    public void testCountJobTaskExecutionsCountsOnlyLatestAttempts() {
        long jobId = saveJob();

        saveTaskExecution(jobId, TaskExecution.Status.COMPLETED);

        TaskExecution failedAttempt = TaskExecution.builder()
            .error(new ExecutionError("failed", List.of()))
            .jobId(jobId)
            .maxRetries(1)
            .retryAttempts(0)
            .status(TaskExecution.Status.FAILED)
            .taskNumber(2)
            .workflowTask(newWorkflowTask("retried"))
            .build();

        taskExecutionRepository.save(failedAttempt);

        TaskExecution retryAttempt = TaskExecution.builder()
            .jobId(jobId)
            .maxRetries(1)
            .retryAttempts(1)
            .status(TaskExecution.Status.COMPLETED)
            .taskNumber(2)
            .workflowTask(newWorkflowTask("retried"))
            .build();

        taskExecutionRepository.save(retryAttempt);

        long supersededId = getId(saveTaskExecution(jobId, TaskExecution.Status.COMPLETED));

        assertThat(taskExecutionService.countJobTaskExecutions(jobId, List.of()))
            .as("the retried failed attempt is not a latest attempt")
            .isEqualTo(3L);
        assertThat(taskExecutionService.countJobTaskExecutions(jobId, List.of(supersededId)))
            .as("superseded rows are excluded as well")
            .isEqualTo(2L);
    }

    @Test
    public void testFetchLastCancelledLeafJobTaskExecutionReadsLastCancelledLeafRow() {
        long jobId = saveJob();

        saveTaskExecution(jobId, TaskExecution.Status.CANCELLED);

        TaskExecution cancelledParent = saveTaskExecution(jobId, TaskExecution.Status.CANCELLED);

        long cancelledLeafId = getId(saveChild(jobId, getId(cancelledParent), 1, TaskExecution.Status.CANCELLED));

        saveTaskExecution(jobId, TaskExecution.Status.COMPLETED);

        assertThat(taskExecutionService.fetchLastCancelledLeafJobTaskExecution(jobId))
            .hasValueSatisfying(taskExecution -> assertThat(taskExecution.getId()).isEqualTo(cancelledLeafId));
    }

    @Test
    public void testFetchLastStartedJobTaskExecutionReadsLastStartedRow() {
        long jobId = saveJob();

        long lastStartedId = getId(saveStartedTaskExecution(jobId, Instant.parse("2026-09-25T10:00:05Z")));

        saveStartedTaskExecution(jobId, Instant.parse("2026-09-25T10:00:00Z"));
        saveTaskExecution(jobId, TaskExecution.Status.CREATED);

        assertThat(taskExecutionService.fetchLastStartedJobTaskExecution(jobId))
            .hasValueSatisfying(taskExecution -> assertThat(taskExecution.getId()).isEqualTo(lastStartedId));
    }

    @Test
    public void testGetChildCountsReadsGroupedCountsPerParent() {
        long jobId = saveJob();

        long firstParentId = saveParentWithChildren(jobId, 0, 1);
        long secondParentId = saveParentWithChildren(jobId, 0);

        assertThat(taskExecutionService.getChildCounts(List.of(firstParentId, secondParentId)))
            .isEqualTo(Map.of(firstParentId, 2L, secondParentId, 1L));
    }

    @Test
    public void testGetIterationCountsReadsGroupedIterationStartCountsPerParent() {
        long jobId = saveJob();

        long firstParentId = saveParentWithChildren(jobId, 0, 1);
        long secondParentId = saveParentWithChildren(jobId, 0, 1, 0, 1);

        assertThat(taskExecutionService.getIterationCounts(List.of(firstParentId, secondParentId)))
            .isEqualTo(Map.of(firstParentId, 1L, secondParentId, 2L));
    }

    @Test
    public void testGetIterationCountsAddTheLeadingIteration() {
        long jobId = saveJob();

        long firstParentId = saveParentWithChildren(jobId, 0, 1);
        long secondParentId = saveParentWithChildren(jobId, 1, 0, 1, 0, 1);
        long thirdParentId = saveParentWithChildren(jobId, 1);

        assertThat(taskExecutionService.getIterationCounts(List.of(firstParentId, secondParentId, thirdParentId)))
            .isEqualTo(Map.of(firstParentId, 1L, secondParentId, 3L, thirdParentId, 1L));
    }

    @Test
    public void testGetParentIterationStartTaskExecutionsReadsTaskNumberZeroRowsAtOffset() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 0, 1, 0, 1, 0, 1, 0);

        assertThat(taskExecutionService.getParentIterationStartTaskExecutions(parentId, 1, 2))
            .extracting(TaskExecution::getId)
            .containsExactly(children.get(2)
                .getId(),
                children.get(4)
                    .getId());
    }

    @Test
    public void testGetParentTaskExecutionsBetweenReadsHalfOpenRange() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 1, 2, 3, 4);

        assertThat(taskExecutionService.getParentTaskExecutionsBetween(parentId, children.get(1), children.get(3)))
            .extracting(TaskExecution::getId)
            .containsExactly(children.get(1)
                .getId(),
                children.get(2)
                    .getId());
    }

    @Test
    public void testGetParentTaskExecutionsFromReadsOpenEndedRange() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 1, 2, 3, 4);

        assertThat(taskExecutionService.getParentTaskExecutionsFrom(parentId, children.get(1)))
            .extracting(TaskExecution::getId)
            .containsExactly(children.get(1)
                .getId(),
                children.get(2)
                    .getId(),
                children.get(3)
                    .getId());
    }

    @Test
    public void testGetTaskExecutionsKeepsRequestedOrder() {
        long jobId = saveJob();

        long firstId = getId(saveTaskExecution(jobId, TaskExecution.Status.COMPLETED));
        long secondId = getId(saveTaskExecution(jobId, TaskExecution.Status.COMPLETED));

        assertThat(taskExecutionService.getTaskExecutions(List.of(secondId, firstId)))
            .extracting(TaskExecution::getId)
            .containsExactly(secondId, firstId);
    }

    @Test
    public void testGetTaskExecutionsSkipsIdsThatAreNotFound() {
        long secondId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.COMPLETED));

        assertThat(taskExecutionService.getTaskExecutions(List.of(-1L, secondId)))
            .extracting(TaskExecution::getId)
            .containsExactly(secondId);
    }

    @Test
    public void testGetTaskExecutionsQueriesEachIdOnce() {
        long firstId = getId(saveTaskExecution(saveJob(), TaskExecution.Status.COMPLETED));

        assertThat(taskExecutionService.getTaskExecutions(List.of(firstId, firstId)))
            .extracting(TaskExecution::getId)
            .containsExactly(firstId, firstId);
    }

    @Test
    public void testGetIterationIndexIsZeroBasedCount() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 0, 1, 0, 1, 0, 1);

        assertThat(taskExecutionService.getIterationIndex(children.get(4))).isEqualTo(2);
        assertThat(taskExecutionService.getIterationIndex(children.get(5))).isEqualTo(2);
    }

    @Test
    public void testGetIterationIndexCountsTheLeadingIteration() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 1, 0, 1);

        assertThat(taskExecutionService.getIterationIndex(children.get(0))).isZero();
        assertThat(taskExecutionService.getIterationIndex(children.get(1))).isEqualTo(1);
    }

    @Test
    public void testGetChildIndexIsZeroBasedPosition() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 1, 2, 3, 4);

        assertThat(taskExecutionService.getChildIndex(children.get(0))).isZero();
        assertThat(taskExecutionService.getChildIndex(children.get(3))).isEqualTo(3);
    }

    @Test
    public void testFetchTaskExecutionIsEmptyForMissingRow() {
        assertThat(taskExecutionService.fetchTaskExecution(-1L)).isEmpty();
    }

    @Test
    public void testGetJobTopLevelTaskExecutionsReadsOnlyTopLevelRows() {
        long jobId = saveJob();

        long topLevelId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        saveChildren(jobId, topLevelId, 1, 2);

        long secondTopLevelId = getId(saveTaskExecution(jobId, TaskExecution.Status.CREATED));

        assertThat(taskExecutionService.getJobTopLevelTaskExecutions(jobId))
            .extracting(TaskExecution::getId)
            .containsExactly(topLevelId, secondTopLevelId);
    }

    @Test
    public void testGetParentTaskExecutionsUsesPageOffset() {
        long jobId = saveJob();
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        List<TaskExecution> children = saveChildren(jobId, parentId, 1, 2, 3, 4, 5);

        assertThat(taskExecutionService.getParentTaskExecutions(parentId, 1, 2))
            .extracting(TaskExecution::getId)
            .containsExactly(children.get(2)
                .getId(),
                children.get(3)
                    .getId());
    }

    private void assertRowLockReleased(long taskExecutionId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Long lockedId = transactionTemplate.execute(transactionStatus -> jdbcTemplate.queryForObject(
            "SELECT id FROM task_execution WHERE id = ? FOR UPDATE NOWAIT", Long.class, taskExecutionId));

        assertThat(lockedId).isEqualTo(taskExecutionId);
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the latch");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private static long getId(TaskExecution taskExecution) {
        return Validate.notNull(taskExecution.getId(), "id");
    }

    private TaskExecution getPersistedTaskExecution(long taskExecutionId) {
        return taskExecutionRepository.findById(taskExecutionId)
            .orElseThrow();
    }

    private static WorkflowTask newWorkflowTask(String name) {
        return new WorkflowTask(Map.of("name", name, "type", "test/v1/noop"));
    }

    private TaskExecution saveChild(long jobId, long parentId, int taskNumber, TaskExecution.Status status) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .parentId(parentId)
            .status(status)
            .taskNumber(taskNumber)
            .workflowTask(newWorkflowTask("child" + taskNumber))
            .build();

        taskExecution = taskExecutionRepository.save(taskExecution);

        return getPersistedTaskExecution(getId(taskExecution));
    }

    private List<TaskExecution> saveChildren(long jobId, long parentId, int... taskNumbers) {
        List<TaskExecution> children = new ArrayList<>();

        for (int taskNumber : taskNumbers) {
            children.add(saveChild(jobId, parentId, taskNumber, TaskExecution.Status.COMPLETED));
        }

        return children;
    }

    private long saveJob() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Validate.notNull(job.getId(), "id");
    }

    private long saveParentWithChildren(long jobId, int... taskNumbers) {
        long parentId = getId(saveTaskExecution(jobId, TaskExecution.Status.STARTED));

        saveChildren(jobId, parentId, taskNumbers);

        return parentId;
    }

    private TaskExecution saveStartedTaskExecution(long jobId, Instant startDate) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .startDate(startDate)
            .status(TaskExecution.Status.STARTED)
            .taskNumber(1)
            .workflowTask(newWorkflowTask("started"))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }

    private TaskExecution saveTaskExecution(long jobId, TaskExecution.Status status) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .status(status)
            .taskNumber(1)
            .workflowTask(newWorkflowTask("task1"))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }
}
