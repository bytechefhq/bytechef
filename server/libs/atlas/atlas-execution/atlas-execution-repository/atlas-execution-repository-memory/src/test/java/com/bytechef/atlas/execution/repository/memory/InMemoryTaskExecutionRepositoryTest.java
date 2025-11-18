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
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @author Ivica Cardic
 */
class InMemoryTaskExecutionRepositoryTest {

    private static final String TASK_EXECUTION_CACHE_NAME =
        InMemoryTaskExecutionRepository.class.getName() + ".taskExecution";

    private InMemoryTaskExecutionRepository inMemoryTaskExecutionRepository;

    @BeforeEach
    void setUp() {
        TenantContext.resetCurrentTenantId();

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(TASK_EXECUTION_CACHE_NAME);

        inMemoryTaskExecutionRepository = new InMemoryTaskExecutionRepository(cacheManager);

        TenantContext.setCurrentTenantId("T");
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
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
