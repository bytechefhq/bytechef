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

package com.bytechef.atlas.execution.repository.memory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.tenant.TenantContext;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class InMemoryTaskExecutionRepositoryTest {

    private InMemoryTaskExecutionRepository inMemoryTaskExecutionRepository;

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        inMemoryTaskExecutionRepository = new InMemoryTaskExecutionRepository();

        TenantContext.setCurrentTenantId("T");
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testFindAllIdsByParentIdOrderByCreatedDate() {
        List<Long> childIds = saveChildren(401L, 400L);

        assertEquals(childIds, inMemoryTaskExecutionRepository.findAllIdsByParentIdOrderByCreatedDate(400L));
    }

    @Test
    void testFindAllByParentIdAndTaskNumberOrderByCreatedDate() {
        List<Long> childIds = saveChildren(411L, 412L);

        assertEquals(
            List.of(childIds.get(2), childIds.get(4)),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndTaskNumberOrderByCreatedDate(412L, 0, 3, 1)));
        assertEquals(
            List.of(childIds.get(0)),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndTaskNumberOrderByCreatedDate(412L, 0, 1, 0)));
    }

    @Test
    void testFindAllByParentIdAndCreatedDateBetweenAndFromOrderByCreatedDate() {
        Instant baseDate = Instant.parse("2026-01-01T00:00:00Z");

        List<Long> childIds = saveChildren(413L, 414L, baseDate);

        assertEquals(
            List.of(childIds.get(2), childIds.get(3)),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateBetweenOrderByCreatedDate(
                414L, baseDate.plusSeconds(2), childIds.get(2), baseDate.plusSeconds(4), childIds.get(4))));
        assertEquals(
            List.of(childIds.get(2), childIds.get(3), childIds.get(4)),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateFromOrderByCreatedDate(
                414L, baseDate.plusSeconds(2), childIds.get(2))));
    }

    @Test
    void testChildRangesBreakTiedCreatedDatesById() {
        Instant createdDate = Instant.parse("2026-01-01T00:00:00Z");

        saveChildren(415L, 416L, createdDate, 0);

        List<TaskExecution> orderedTaskExecutions = inMemoryTaskExecutionRepository
            .findAllByParentIdOrderByCreatedDate(416L, 5, 0);

        List<Long> orderedIds = toIds(orderedTaskExecutions);

        assertEquals(
            orderedIds.stream()
                .sorted()
                .toList(),
            orderedIds);

        List<Long> iterationStartIds = orderedTaskExecutions.stream()
            .filter(taskExecution -> taskExecution.getTaskNumber() == 0)
            .map(TaskExecution::getId)
            .toList();

        assertEquals(
            iterationStartIds.subList(1, 3),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndTaskNumberOrderByCreatedDate(416L, 0, 2, 1)));
        assertEquals(
            orderedIds.subList(1, 3),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateBetweenOrderByCreatedDate(
                416L, createdDate, orderedIds.get(1), createdDate, orderedIds.get(3))));
        assertEquals(
            orderedIds.subList(3, 5),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateFromOrderByCreatedDate(
                416L, createdDate, orderedIds.get(3))));
    }

    @Test
    void testFindAllByParentIdOrderByCreatedDatePagesChildren() {
        List<Long> childIds = saveChildren(402L, 410L);

        List<TaskExecution> taskExecutions =
            inMemoryTaskExecutionRepository.findAllByParentIdOrderByCreatedDate(410L, 2, 2);

        assertEquals(
            List.of(childIds.get(2), childIds.get(3)),
            taskExecutions.stream()
                .map(TaskExecution::getId)
                .toList());
    }

    @Test
    void testCountAllByParentIdIn() {
        saveChildren(403L, 420L);
        saveChildren(403L, 421L);

        assertEquals(
            Map.of(420L, 5L, 421L, 5L),
            inMemoryTaskExecutionRepository.countAllByParentIdIn(List.of(420L, 421L, 422L)));
        assertEquals(
            Map.of(420L, 3L),
            inMemoryTaskExecutionRepository.countAllByParentIdInAndTaskNumber(List.of(420L, 422L), 0));
        assertTrue(inMemoryTaskExecutionRepository.countAllByParentIdIn(List.of())
            .isEmpty());
    }

    @Test
    void testFindAllByIdInAndCounts() {
        List<Long> childIds = saveChildren(404L, 430L);

        assertEquals(
            2, inMemoryTaskExecutionRepository.findAllByIdIn(List.of(childIds.get(1), childIds.get(3)))
                .size());
    }

    @Test
    void testCountLatestAttemptsByJobId() {
        TaskExecution retriedLoopTaskExecution = saveTaskExecution(404L, null, true);

        saveTaskExecution(404L, retriedLoopTaskExecution.getId(), false);
        saveTaskExecution(404L, retriedLoopTaskExecution.getId(), false);

        TaskExecution loopRetryTaskExecution = saveTaskExecution(404L, null, false);

        saveTaskExecution(404L, loopRetryTaskExecution.getId(), false);
        saveTaskExecution(404L, loopRetryTaskExecution.getId(), true);
        saveTaskExecution(404L, loopRetryTaskExecution.getId(), false);

        assertEquals(3, inMemoryTaskExecutionRepository.countLatestAttemptsByJobId(404L, List.of()));
        assertEquals(0, inMemoryTaskExecutionRepository.countLatestAttemptsByJobId(405L, List.of()));
    }

    @Test
    void testCountLatestAttemptsByJobIdExcludesSupersededAttempts() {
        TaskExecution resumedLoopTaskExecution = saveFailedTaskExecution(408L, null);

        TaskExecution resumedIterationTaskExecution = saveTaskExecution(
            408L, resumedLoopTaskExecution.getId(), false);

        saveTaskExecution(408L, resumedIterationTaskExecution.getId(), false);
        saveTaskExecution(408L, resumedLoopTaskExecution.getId(), true);

        TaskExecution loopTaskExecution = saveTaskExecution(408L, null, false);

        saveTaskExecution(408L, loopTaskExecution.getId(), false);

        assertEquals(
            2, inMemoryTaskExecutionRepository.countLatestAttemptsByJobId(
                408L, List.of(resumedLoopTaskExecution.getId())));
        assertEquals(5, inMemoryTaskExecutionRepository.countLatestAttemptsByJobId(408L, List.of()));
    }

    @Test
    void testCountByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual() {
        Instant baseDate = Instant.parse("2026-01-01T00:00:00Z");

        List<Long> childIds = saveChildren(407L, 450L, baseDate);

        assertEquals(
            2,
            inMemoryTaskExecutionRepository.countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
                450L, 0, baseDate.plusSeconds(2), childIds.get(2)));
        assertEquals(
            0,
            inMemoryTaskExecutionRepository.countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
                450L, 0, baseDate.minusSeconds(1), childIds.get(0)));
    }

    @Test
    void testCountByParentIdAndCreatedDateBeforeOrEqual() {
        Instant baseDate = Instant.parse("2026-01-01T00:00:00Z");

        List<Long> childIds = saveChildren(408L, 455L, baseDate);

        assertEquals(
            3,
            inMemoryTaskExecutionRepository.countByParentIdAndCreatedDateBeforeOrEqual(
                455L, baseDate.plusSeconds(2), childIds.get(2)));
        assertEquals(
            0,
            inMemoryTaskExecutionRepository.countByParentIdAndCreatedDateBeforeOrEqual(
                455L, baseDate.minusSeconds(1), childIds.get(0)));
    }

    @Test
    void testFindAllIdsByIdInAndFirstChildTaskNumberNot() {
        saveChildren(409L, 456L);

        TaskExecution leadingTaskExecution = new TaskExecution();

        leadingTaskExecution.setJobId(409L);
        leadingTaskExecution.setParentId(457L);
        leadingTaskExecution.setTaskNumber(1);

        inMemoryTaskExecutionRepository.save(leadingTaskExecution);

        assertEquals(
            List.of(457L),
            inMemoryTaskExecutionRepository.findAllIdsByIdInAndFirstChildTaskNumberNot(List.of(456L, 457L, 458L), 0));
        assertEquals(List.of(),
            inMemoryTaskExecutionRepository.findAllIdsByIdInAndFirstChildTaskNumberNot(List.of(), 0));
    }

    @Test
    void testFindAllLatestAttemptFailedLeavesByJobIdOrderByCreatedDate() {
        saveChildren(405L, 440L);

        TaskExecution retriedLoopTaskExecution = saveTaskExecution(406L, null, true);

        saveFailedTaskExecution(406L, retriedLoopTaskExecution.getId());

        TaskExecution loopTaskExecution = saveTaskExecution(406L, null, false);

        saveTaskExecution(406L, loopTaskExecution.getId(), true);

        TaskExecution firstFailedLeafTaskExecution = saveFailedTaskExecution(406L, loopTaskExecution.getId());
        TaskExecution failedParentTaskExecution = saveFailedTaskExecution(406L, loopTaskExecution.getId());

        saveTaskExecution(406L, failedParentTaskExecution.getId(), false);

        TaskExecution secondFailedLeafTaskExecution = saveFailedTaskExecution(406L, null);

        saveTaskExecution(406L, null, false);

        assertEquals(
            List.of(firstFailedLeafTaskExecution.getId(), secondFailedLeafTaskExecution.getId()),
            toIds(inMemoryTaskExecutionRepository.findAllLatestAttemptFailedLeavesByJobIdOrderByCreatedDate(
                406L, 10, 0)));
        assertEquals(
            List.of(secondFailedLeafTaskExecution.getId()),
            toIds(inMemoryTaskExecutionRepository.findAllLatestAttemptFailedLeavesByJobIdOrderByCreatedDate(
                406L, 1, 1)));
    }

    @Test
    void testFindAllByJobIdAndParentIdIsNullOrderByCreatedDate() {
        List<Long> topLevelIds = new ArrayList<>();

        for (int index = 0; index < 3; index++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(409L);

            TaskExecution savedTaskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

            topLevelIds.add(savedTaskExecution.getId());
        }

        saveChildren(409L, topLevelIds.getFirst());

        assertEquals(
            topLevelIds,
            inMemoryTaskExecutionRepository.findAllByJobIdAndParentIdIsNullOrderByCreatedDate(409L)
                .stream()
                .map(TaskExecution::getId)
                .toList());
    }

    @Test
    void testChildrenWithoutCreatedDateKeepInsertionOrder() {
        List<Long> childIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (int index = 0; index < 20; index++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(408L);
            taskExecution.setParentId(460L);
            taskExecution.setTaskNumber(index % 2);

            if (index % 3 == 0) {
                taskExecution.setError(new ExecutionError("failed", List.of()));
            }

            TaskExecution savedTaskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

            childIds.add(savedTaskExecution.getId());

            if (index % 3 == 0) {
                failedIds.add(savedTaskExecution.getId());
            }
        }

        assertEquals(childIds, inMemoryTaskExecutionRepository.findAllIdsByParentIdOrderByCreatedDate(460L));
        assertEquals(
            childIds.subList(5, 9),
            inMemoryTaskExecutionRepository.findAllByParentIdOrderByCreatedDate(460L, 4, 5)
                .stream()
                .map(TaskExecution::getId)
                .toList());
        assertEquals(
            childIds.subList(5, 9),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateBetweenOrderByCreatedDate(
                460L, null, childIds.get(5), null, childIds.get(9))));
        assertEquals(
            childIds.subList(15, 20),
            toIds(inMemoryTaskExecutionRepository.findAllByParentIdAndCreatedDateFromOrderByCreatedDate(
                460L, null, childIds.get(15))));
        assertEquals(
            failedIds,
            toIds(inMemoryTaskExecutionRepository.findAllLatestAttemptFailedLeavesByJobIdOrderByCreatedDate(
                408L, 100, 0)));
    }

    @Test
    void testFindLastStartedByJobId() {
        TaskExecution firstStartedTaskExecution = new TaskExecution();

        firstStartedTaskExecution.setJobId(470L);
        firstStartedTaskExecution.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));

        inMemoryTaskExecutionRepository.save(firstStartedTaskExecution);

        TaskExecution lastStartedTaskExecution = new TaskExecution();

        lastStartedTaskExecution.setJobId(470L);
        lastStartedTaskExecution.setStartDate(Instant.parse("2026-01-01T00:00:01Z"));

        TaskExecution savedLastStartedTaskExecution = inMemoryTaskExecutionRepository.save(lastStartedTaskExecution);

        TaskExecution createdTaskExecution = new TaskExecution();

        createdTaskExecution.setJobId(470L);

        TaskExecution savedCreatedTaskExecution = inMemoryTaskExecutionRepository.save(createdTaskExecution);

        assertEquals(
            savedCreatedTaskExecution.getId(), inMemoryTaskExecutionRepository.findLastByJobId(470L)
                .map(TaskExecution::getId)
                .orElseThrow());
        assertEquals(
            savedLastStartedTaskExecution.getId(), inMemoryTaskExecutionRepository.findLastStartedByJobId(470L)
                .map(TaskExecution::getId)
                .orElseThrow());

        TaskExecution notStartedTaskExecution = new TaskExecution();

        notStartedTaskExecution.setJobId(471L);

        inMemoryTaskExecutionRepository.save(notStartedTaskExecution);

        assertTrue(inMemoryTaskExecutionRepository.findLastStartedByJobId(471L)
            .isEmpty());
    }

    private List<Long> saveChildren(long jobId, long parentId) {
        return saveChildren(jobId, parentId, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private List<Long> saveChildren(long jobId, long parentId, Instant baseDate) {
        return saveChildren(jobId, parentId, baseDate, 1);
    }

    private List<Long> saveChildren(long jobId, long parentId, Instant baseDate, long createdDateStepSeconds) {
        List<Long> childIds = new ArrayList<>();
        int[] taskNumbers = {
            0, 1, 0, 1, 0
        };

        for (int index = 0; index < taskNumbers.length; index++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);
            taskExecution.setParentId(parentId);
            taskExecution.setTaskNumber(taskNumbers[index]);

            try {
                Field createdDateField = TaskExecution.class.getDeclaredField("createdDate");

                createdDateField.setAccessible(true);
                createdDateField.set(taskExecution, baseDate.plusSeconds(index * createdDateStepSeconds));
            } catch (ReflectiveOperationException reflectiveOperationException) {
                throw new IllegalStateException(reflectiveOperationException);
            }

            TaskExecution savedTaskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

            childIds.add(savedTaskExecution.getId());
        }

        return childIds;
    }

    private static List<Long> toIds(List<TaskExecution> taskExecutions) {
        return taskExecutions.stream()
            .map(TaskExecution::getId)
            .toList();
    }

    @Test
    void testConcurrentParentIndexingParentFinderRemainsConsistent() throws Exception {
        long jobId = 301L;
        long parentId = 999L;
        int threads = 6;
        int perThread = 25;

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                final int base = t * perThread;
                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("A");
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            TaskExecution te = new TaskExecution();
                            te.setJobId(jobId);
                            te.setParentId(parentId);
                            te.setTaskNumber(base + i);
                            inMemoryTaskExecutionRepository.save(te);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));

            TenantContext.runWithTenantId("A", () -> {
                List<TaskExecution> byParent =
                    inMemoryTaskExecutionRepository.findAllByParentIdOrderByTaskNumber(parentId);
                assertEquals(threads * perThread, byParent.size());

                // Ensure ordering by taskNumber is correct
                List<Integer> nums = new ArrayList<>();
                for (TaskExecution te : byParent)
                    nums.add(te.getTaskNumber());
                List<Integer> sorted = new ArrayList<>(nums);
                sorted.sort(Integer::compareTo);
                assertEquals(sorted, nums);
            });
        }
    }

    @Test
    void testConcurrentSavesDifferentJobsIsolatedAndAccurate() throws Exception {
        long job1 = 201L;
        long job2 = 202L;
        int perJob = 100;

        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            pool.submit(() -> {
                TenantContext.setCurrentTenantId("A");
                try {
                    start.await();
                    for (int i = 0; i < perJob; i++) {
                        TaskExecution te = new TaskExecution();
                        te.setJobId(job1);
                        te.setTaskNumber(i);
                        inMemoryTaskExecutionRepository.save(te);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });

            pool.submit(() -> {
                TenantContext.setCurrentTenantId("A");
                try {
                    start.await();
                    for (int i = 0; i < perJob; i++) {
                        TaskExecution te = new TaskExecution();
                        te.setJobId(job2);
                        te.setTaskNumber(i);
                        inMemoryTaskExecutionRepository.save(te);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });

            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));

            TenantContext.runWithTenantId("A", () -> {
                assertEquals(perJob, inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(job1)
                    .size());
                assertEquals(perJob, inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(job2)
                    .size());
            });
        }
    }

    @Test
    void testConcurrentSavesToSameJobThenFindersAreConsistent() throws Exception {
        long jobId = 101L;
        int writers = 8;
        int perWriter = 50;
        int expected = writers * perWriter;

        try (ExecutorService pool = Executors.newFixedThreadPool(writers + 2)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writers);
            AtomicReference<Throwable> readerFailure = new AtomicReference<>();

            // Writer tasks
            for (int w = 0; w < writers; w++) {
                final int writerIndex = w;

                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("A");

                    try {
                        assertTrue(start.await(5, TimeUnit.SECONDS), "writers did not start in time");

                        for (int i = 0; i < perWriter; i++) {
                            TaskExecution taskExecution = new TaskExecution();

                            taskExecution.setJobId(jobId);
                            // Give unique-ish task numbers across writers
                            taskExecution.setTaskNumber(writerIndex * perWriter + i);

                            inMemoryTaskExecutionRepository.save(taskExecution);
                        }
                    } catch (InterruptedException ie) {
                        Thread currentThread = Thread.currentThread();

                        currentThread.interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            // Reader polling concurrently
            Future<?> reader = pool.submit(() -> {
                TenantContext.setCurrentTenantId("A");

                try {
                    assertTrue(start.await(5, TimeUnit.SECONDS), "reader did not start in time");

                    int lastSeen = 0;

                    while (done.getCount() > 0) {
                        List<TaskExecution> snapshotTaskExecutions = inMemoryTaskExecutionRepository
                            .findAllByJobIdOrderByCreatedDate(jobId);

                        // Ensure no nulls and stable snapshot
                        assertNotNull(snapshotTaskExecutions);
                        assertTrue(
                            snapshotTaskExecutions.size() >= lastSeen, "sizes should be non-decreasing while writing");

                        lastSeen = snapshotTaskExecutions.size();

                        // Also probe other finders
                        assertDoesNotThrow(
                            () -> inMemoryTaskExecutionRepository.findAllByJobIdOrderByTaskNumber(jobId));
                        assertDoesNotThrow(() -> inMemoryTaskExecutionRepository.findAllByJobIdOrderByIdDesc(jobId));
                    }
                } catch (Throwable t) {
                    readerFailure.set(t);
                }
            });

            // Start everyone
            start.countDown();

            assertTrue(done.await(20, TimeUnit.SECONDS), "writers finished in time");
            reader.get(5, TimeUnit.SECONDS);
            assertNull(readerFailure.get(), () -> "reader failed: " + readerFailure.get());

            // Final assertions under the same tenant
            TenantContext.runWithTenantId("A", () -> {
                List<TaskExecution> byCreatedTaskExecutions = inMemoryTaskExecutionRepository
                    .findAllByJobIdOrderByCreatedDate(jobId);

                assertEquals(expected, byCreatedTaskExecutions.size());

                List<TaskExecution> byTaskNumTaskExecutions = inMemoryTaskExecutionRepository
                    .findAllByJobIdOrderByTaskNumber(jobId);

                assertEquals(expected, byTaskNumTaskExecutions.size());

                // task numbers must be unique and cover the expected range
                Set<Integer> taskNumbers = new HashSet<>();

                for (TaskExecution te : byTaskNumTaskExecutions) {
                    taskNumbers.add(te.getTaskNumber());
                }

                assertEquals(expected, taskNumbers.size());

                // findLast should return some element from the job list
                TaskExecution lastTaskExecution = inMemoryTaskExecutionRepository.findLastByJobId(jobId)
                    .orElseThrow();

                assertTrue(byCreatedTaskExecutions.contains(lastTaskExecution));
            });
        }
    }

    @Test
    void testCrossTenantIsolationNoLeakage() {
        long jobId = 730L;

        // tenant A writes 3
        TenantContext.setCurrentTenantId("A");

        for (int i = 0; i < 3; i++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);

            inMemoryTaskExecutionRepository.save(taskExecution);
        }

        // tenant B writes 5
        TenantContext.setCurrentTenantId("B");

        for (int i = 0; i < 5; i++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);

            inMemoryTaskExecutionRepository.save(taskExecution);
        }

        // assert isolation
        TenantContext.setCurrentTenantId("A");

        List<TaskExecution> taskExecutions = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(jobId);

        assertEquals(3, taskExecutions.size());

        TenantContext.setCurrentTenantId("B");

        taskExecutions = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(jobId);

        assertEquals(5, taskExecutions.size());
    }

    @Test
    void testFindLastByJobIdReturnsLastAppendedUnderConcurrentAppends() throws Exception {
        long jobId = 710L;
        int writers = 4;
        int perWriter = 10;

        try (ExecutorService pool = Executors.newFixedThreadPool(writers)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writers);

            for (int writer = 0; writer < writers; writer++) {
                final int offset = writer * perWriter;

                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("T");

                    try {
                        assertTrue(start.await(2, TimeUnit.SECONDS), "writers did not start in time");

                        for (int i = 0; i < perWriter; i++) {
                            TaskExecution taskExecution = new TaskExecution();

                            taskExecution.setJobId(jobId);
                            taskExecution.setTaskNumber(offset + i);

                            inMemoryTaskExecutionRepository.save(taskExecution);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();

            assertTrue(done.await(10, TimeUnit.SECONDS));

            List<TaskExecution> byCreatedTaskExecutions = inMemoryTaskExecutionRepository
                .findAllByJobIdOrderByCreatedDate(jobId);

            TaskExecution lastTaskExecution = inMemoryTaskExecutionRepository.findLastByJobId(jobId)
                .orElseThrow();

            TaskExecution byCreatedLastTaskExecution = byCreatedTaskExecutions.getLast();

            assertEquals(byCreatedLastTaskExecution.getId(), lastTaskExecution.getId());
        }
    }

    @Test
    void testIdDescOrderingStrictlyDescending() {
        long jobId = 720L;
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);

            taskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

            ids.add(taskExecution.getId());
        }

        List<TaskExecution> byIdDesc = inMemoryTaskExecutionRepository.findAllByJobIdOrderByIdDesc(jobId);

        // verify the strictly descending sequence
        long prevId = Long.MAX_VALUE;

        for (TaskExecution taskExecution : byIdDesc) {
            assertTrue(Objects.requireNonNull(taskExecution.getId()) <= prevId, "Ids must be in descending order");

            prevId = Objects.requireNonNull(taskExecution.getId());
        }

        assertEquals(new HashSet<>(ids).size(), new HashSet<>(getIds(byIdDesc)).size());
    }

    @Test
    void testInsertionOrderPreservedUpdatesReplaceInPlaceJobAndParentLists() {
        long jobId = 700L;
        long parentId = 701L;

        TaskExecution taskExecution1 = new TaskExecution();

        taskExecution1.setJobId(jobId);
        taskExecution1.setParentId(parentId);
        taskExecution1.setTaskNumber(0);

        TaskExecution taskExecution2 = new TaskExecution();

        taskExecution2.setJobId(jobId);
        taskExecution2.setParentId(parentId);
        taskExecution2.setTaskNumber(1);

        TaskExecution taskExecution3 = new TaskExecution();

        taskExecution3.setJobId(jobId);
        taskExecution3.setParentId(parentId);
        taskExecution3.setTaskNumber(2);

        taskExecution1 = inMemoryTaskExecutionRepository.save(taskExecution1);
        taskExecution2 = inMemoryTaskExecutionRepository.save(taskExecution2);
        taskExecution3 = inMemoryTaskExecutionRepository.save(taskExecution3);

        // update middle twice
        taskExecution2.setStatus(TaskExecution.Status.STARTED);

        inMemoryTaskExecutionRepository.save(taskExecution2);

        taskExecution2.setStatus(TaskExecution.Status.COMPLETED);

        inMemoryTaskExecutionRepository.save(taskExecution2);

        List<TaskExecution> byCreatedTaskExecutions =
            inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(jobId);

        assertEquals(
            List.of(
                Objects.requireNonNull(taskExecution1.getId()), Objects.requireNonNull(taskExecution2.getId()),
                Objects.requireNonNull(taskExecution3.getId())),
            getIds(byCreatedTaskExecutions), "Updates must not reorder insertion order in job list");

        List<TaskExecution> byParentTaskExecutions = inMemoryTaskExecutionRepository.findAllByParentIdOrderByTaskNumber(
            parentId);

        assertEquals(List.of(0, 1, 2), getTaskNumbers(byParentTaskExecutions),
            "Parent list must remain sorted by taskNumber after updates");

        // ensure single entry per id
        assertEquals(3, new HashSet<>(getIds(byCreatedTaskExecutions)).size());
        assertEquals(3, new HashSet<>(getIds(byParentTaskExecutions)).size());
    }

    @Test
    void testMixedParentNullNonNullNoDeadlocks() throws Exception {
        long jobId = 740L;

        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            CountDownLatch latch = new CountDownLatch(40);

            for (int i = 0; i < 40; i++) {
                final int idx = i;

                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("T");

                    try {
                        TaskExecution taskExecution = new TaskExecution();

                        taskExecution.setJobId(jobId);

                        if (idx % 2 == 0) {
                            taskExecution.setParentId(1000L + (idx % 4));
                            taskExecution.setTaskNumber(idx % 3);
                        }

                        inMemoryTaskExecutionRepository.save(taskExecution);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS), "All writers finished in time");
        }
    }

    @Test
    void testSingleIdConcurrentUpdatesNoDuplicates() throws Exception {
        TenantContext.setCurrentTenantId("A");

        long jobId = 43L;
        long parentId = 4301L;

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);
        taskExecution.setTaskNumber(0);

        taskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

        int writers = 8;

        try (ExecutorService pool = Executors.newFixedThreadPool(writers)) {
            CountDownLatch latch = new CountDownLatch(writers);

            final TaskExecution seedTaskExecution = taskExecution;

            for (int i = 0; i < writers; i++) {
                final int idx = i;
                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("A");
                    try {
                        TaskExecution copyTaskExecution = seedTaskExecution.clone();

                        // flip status back and forth across writers
                        copyTaskExecution.setStatus((idx % 2 == 0)
                            ? TaskExecution.Status.STARTED : TaskExecution.Status.COMPLETED);

                        inMemoryTaskExecutionRepository.save(copyTaskExecution);
                    } catch (CloneNotSupportedException e) {
                        fail(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS), "writers finished in time");

            List<TaskExecution> byJob = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(jobId);

            assertEquals(1, byJob.size(), "Concurrent updates must not create duplicates in job list");

            List<TaskExecution> byParent = inMemoryTaskExecutionRepository.findAllByParentIdOrderByTaskNumber(parentId);

            assertEquals(1, byParent.size(), "Concurrent updates must not create duplicates in parent list");
        }
    }

    @Test
    void testSingleIdSequentialUpdatesNoDuplicatesAndLatestWins() {
        TenantContext.setCurrentTenantId("A");

        long jobId = 42L;
        long parentId = 4201L;

        // create
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);
        taskExecution.setTaskNumber(0);

        taskExecution = inMemoryTaskExecutionRepository.save(taskExecution);

        // STARTED update
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        inMemoryTaskExecutionRepository.save(taskExecution);

        // COMPLETED update
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        inMemoryTaskExecutionRepository.save(taskExecution);

        List<TaskExecution> byJobTaskExecutions = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(
            jobId);

        assertEquals(1, byJobTaskExecutions.size(), "Must keep a single entry per id in job list");

        TaskExecution byJobTaskExecutionFirst = byJobTaskExecutions.getFirst();

        assertEquals(TaskExecution.Status.COMPLETED, byJobTaskExecutionFirst.getStatus());

        List<TaskExecution> byParentTaskExecutions = inMemoryTaskExecutionRepository.findAllByParentIdOrderByTaskNumber(
            parentId);

        assertEquals(1, byParentTaskExecutions.size(), "Must keep a single entry per id in parent list");

        TaskExecution byParentTaskExecutionFirst = byParentTaskExecutions.getFirst();

        assertEquals(TaskExecution.Status.COMPLETED, byParentTaskExecutionFirst.getStatus());
    }

    @Test
    void testTenantIsolationNoLeakageAcrossTenants() {
        long jobId = 777L;

        // Tenant A saves
        TenantContext.runWithTenantId("A", () -> {
            for (int i = 0; i < 10; i++) {
                TaskExecution taskExecution = new TaskExecution();

                taskExecution.setJobId(jobId);
                taskExecution.setTaskNumber(i);

                inMemoryTaskExecutionRepository.save(taskExecution);
            }
        });

        // Tenant B saves
        TenantContext.runWithTenantId("B", () -> {
            for (int i = 0; i < 7; i++) {
                TaskExecution taskExecution = new TaskExecution();

                taskExecution.setJobId(jobId);
                taskExecution.setTaskNumber(i);

                inMemoryTaskExecutionRepository.save(taskExecution);
            }
        });

        // Verify isolation
        TenantContext.runWithTenantId("A", () -> {
            List<TaskExecution> taskExecutions = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(
                jobId);

            assertEquals(10, taskExecutions.size());
        });
        TenantContext.runWithTenantId("B", () -> {
            List<TaskExecution> taskExecutions = inMemoryTaskExecutionRepository.findAllByJobIdOrderByCreatedDate(
                jobId);

            assertEquals(7, taskExecutions.size());
        });
    }

    private TaskExecution saveTaskExecution(long jobId, Long parentId, boolean retried) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);

        if (retried) {
            taskExecution.setError(new ExecutionError("failed", List.of()));
            taskExecution.setMaxRetries(1);
            taskExecution.setStatus(TaskExecution.Status.FAILED);
        }

        return inMemoryTaskExecutionRepository.save(taskExecution);
    }

    private TaskExecution saveFailedTaskExecution(long jobId, Long parentId) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setError(new ExecutionError("failed", List.of()));
        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);
        taskExecution.setStatus(TaskExecution.Status.FAILED);

        return inMemoryTaskExecutionRepository.save(taskExecution);
    }

    private static List<Long> getIds(List<TaskExecution> list) {
        List<Long> ids = new ArrayList<>();

        for (TaskExecution te : list) {
            ids.add(te.getId());
        }

        return ids;
    }

    private static List<Integer> getTaskNumbers(List<TaskExecution> taskExecutions) {
        List<Integer> taskNumbers = new ArrayList<>();

        for (TaskExecution taskExecution : taskExecutions) {
            taskNumbers.add(taskExecution.getTaskNumber());
        }

        return taskNumbers;
    }
}
