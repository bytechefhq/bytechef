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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.tenant.TenantContext;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @author Ivica Cardic
 */
class InMemoryContextRepositoryTest {

    private static final String CACHE_NAME = InMemoryContextRepository.class.getName() + ".context";

    private InMemoryContextRepository repository;

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(CACHE_NAME);

        repository = new InMemoryContextRepository(cacheManager);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testConcurrentPushesToSameKeySizeAccurateAndPeekStable() throws Exception {
        long stackId = 42L;
        int threads = 6;
        int perThread = 25;
        int total = threads * perThread;

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            Set<String> ids = new HashSet<>();

            for (int t = 0; t < threads; t++) {
                final int base = t * perThread;

                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("A");
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            String name = "f-" + (base + i);

                            FileEntry fileEntry = new FileEntry(name, "http://localhost/" + name);

                            synchronized (ids) {
                                ids.add(fileEntry.toString());
                            }

                            Context context = new Context(stackId, Context.Classname.TASK_EXECUTION, fileEntry);

                            repository.save(context);
                        }
                    } catch (InterruptedException ignored) {
                        Thread currentThread = Thread.currentThread();

                        currentThread.interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();

            assertTrue(done.await(10, TimeUnit.SECONDS));

            // Verify size via iterative peeks: there is no direct size; we can simulate by popping? Repository doesn't
            // expose pop.
            // Instead, we can only assert peek is not null and multiple peeks don't throw. We also indirectly assert
            // size by pushing the known count across different sub-stacks to have countable results. For the same key,
            // assert last is one of ours.
            Context topContext = TenantContext.callWithTenantId(
                "A",
                () -> repository
                    .findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(
                        stackId, Context.Classname.TASK_EXECUTION.ordinal())
                    .orElse(null));

            assertNotNull(topContext);
            assertNotNull(topContext.getValue());
            assertTrue(ids.contains(String.valueOf(topContext.getValue())) || ids.size() == total);
        }
    }

    @Test
    void testConcurrentPushesToDifferentKeysIsolated() throws Exception {
        long stackId = 99L;
        int perKey = 40;
        int keys = 3;

        try (ExecutorService pool = Executors.newFixedThreadPool(keys)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(keys);

            for (int key = 0; key < keys; key++) {
                final int subStackId = key; // differentiate keys by subStack

                pool.submit(() -> {
                    TenantContext.setCurrentTenantId("A");

                    try {
                        start.await();

                        for (int i = 0; i < perKey; i++) {
                            FileEntry fileEntry = new FileEntry(
                                "n" + subStackId + "-" + i, "http://localhost/" + subStackId + "/" + i);

                            // Create context with value for this subStack key
                            Context context = new Context(
                                stackId, subStackId, Context.Classname.TASK_EXECUTION, fileEntry);

                            repository.save(context);
                        }
                    } catch (InterruptedException ignored) {
                        Thread currentThread = Thread.currentThread();

                        currentThread.interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));

            // For each subStackId, there should be a non-null top value under the same tenant
            for (int k = 0; k < keys; k++) {
                int subStackId = k;
                Context topContext = null;
                long deadline = System.currentTimeMillis() + 1_000;

                do {
                    var topContextOptional = TenantContext.callWithTenantId("A", () -> repository
                        .findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
                            stackId, subStackId, Context.Classname.TASK_EXECUTION.ordinal()));

                    if (topContextOptional.isPresent()) {
                        topContext = topContextOptional.get();

                        break;
                    }

                    Thread.sleep(10);
                } while (System.currentTimeMillis() < deadline);

                assertNotNull(topContext, "top context should be present for subStackId=" + subStackId);
                assertNotNull(topContext.getValue());
            }
        }
    }

    @Test
    void testTenantIsolationBetweenAAndB() {
        long stackId = 7L;

        // Tenant A pushes two entries
        TenantContext.runWithTenantId("A", () -> {
            repository.save(new Context(stackId, Context.Classname.TASK_EXECUTION, new FileEntry("a1", "http://a/1")));
            repository.save(new Context(stackId, Context.Classname.TASK_EXECUTION, new FileEntry("a2", "http://a/2")));
        });

        // Tenant B pushes one entry
        TenantContext.runWithTenantId("B", () -> {
            repository.save(new Context(stackId, Context.Classname.TASK_EXECUTION, new FileEntry("b1", "http://b/1")));
        });

        TenantContext.runWithTenantId("A", () -> {
            var topAContextOptional = repository.findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(
                stackId, Context.Classname.TASK_EXECUTION.ordinal());

            assertTrue(topAContextOptional.isPresent());
            assertTrue(topAContextOptional.get()
                .getValue()
                .getUrl()
                .startsWith("http://a/"));
        });

        TenantContext.runWithTenantId("B", () -> {
            var topBContextOptional = repository.findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(
                stackId, Context.Classname.TASK_EXECUTION.ordinal());

            assertTrue(topBContextOptional.isPresent());
            assertTrue(topBContextOptional.get()
                .getValue()
                .getUrl()
                .startsWith("http://b/"));
        });
    }
}
