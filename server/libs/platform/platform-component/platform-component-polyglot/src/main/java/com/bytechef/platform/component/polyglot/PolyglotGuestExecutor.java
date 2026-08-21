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

package com.bytechef.platform.component.polyglot;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runs guest polyglot executions on platform threads.
 *
 * <p>
 * GraalVM enforces {@code sandbox.MaxCPUTime} and {@code sandbox.MaxHeapMemory} through
 * {@link java.lang.management.ThreadMXBean#getThreadCpuTime(long)} and
 * {@code com.sun.management.ThreadMXBean.getThreadAllocatedBytes(long)}, measured on the thread that builds and runs
 * the context. The JDK does not account CPU time or allocation per virtual thread - both calls return {@code -1} - so
 * GraalVM refuses to build such a context at all, failing with "ThreadMXBean.getThreadCpuTime() is not supported or
 * enabled by the host VM". Since this application runs on virtual threads
 * ({@code spring.threads.virtual.enabled=true}), every guest execution that carries a thread-metered ceiling has to be
 * moved onto a platform thread, and the calling virtual thread parks until it finishes.
 *
 * <p>
 * The pool is bounded because guest scripts are the one workload here that can be deliberately CPU-bound: a runaway
 * loop does not yield, so on a virtual thread it pins its carrier for as long as it runs. Queueing beyond the bound is
 * admission control, not a stall - the CPU ceiling caps how long any single execution can hold its slot.
 *
 * @author Ivica Cardic
 */
final class PolyglotGuestExecutor {

    private static final AtomicLong THREAD_COUNT = new AtomicLong();

    /** Idle guest threads are reaped; a quiet instance keeps none. */
    private static final long KEEP_ALIVE_SECONDS = 60;

    private final ExecutorService executorService;

    PolyglotGuestExecutor(int maxConcurrentExecutions) {
        // Core and maximum are equal, with an unbounded queue: a ThreadPoolExecutor only grows past its core size
        // once the queue is full, so an unbounded queue plus a larger maximum would never start a second thread.
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            maxConcurrentExecutions, maxConcurrentExecutions, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), PolyglotGuestExecutor::newGuestThread);

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        this.executorService = threadPoolExecutor;
    }

    /**
     * Runs the given callable on a guest platform thread and blocks until it returns.
     *
     * <p>
     * Thread-bound state the guest reaches back into the platform with - tenant, environment, tracing and the Spring
     * Security context, each registered as a Micrometer {@code ThreadLocalAccessor} - is carried across the hop; the
     * component actions a script invokes are authorized and tenant-scoped exactly as they are on the calling thread.
     *
     * @param callable the guest execution
     * @return whatever the callable returned
     */
    <V> V call(Callable<V> callable) {
        ContextSnapshot contextSnapshot = ContextSnapshotFactory.builder()
            .build()
            .captureAll();

        Future<V> future = executorService.submit(() -> {
            ContextSnapshot.Scope scope = contextSnapshot.setThreadLocals();

            try {
                return callable.call();
            } finally {
                scope.close();
            }
        });

        try {
            return future.get();
        } catch (InterruptedException interruptedException) {
            future.cancel(true);

            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            throw new IllegalStateException("Guest execution interrupted", interruptedException);
        } catch (ExecutionException executionException) {
            throw toRuntimeException(executionException.getCause());
        }
    }

    /**
     * Stops accepting new executions; the ones already running are left to finish.
     */
    void shutdown() {
        executorService.shutdown();
    }

    private static Thread newGuestThread(Runnable runnable) {
        return Thread.ofPlatform()
            .name("polyglot-guest-" + THREAD_COUNT.incrementAndGet())
            .daemon(true)
            .unstarted(runnable);
    }

    /**
     * Unwraps the {@link ExecutionException} so callers see what the guest actually threw - a
     * {@link org.graalvm.polyglot.PolyglotException} carrying the hit limit, most of the time - rather than the
     * plumbing of the hop.
     */
    private static RuntimeException toRuntimeException(Throwable throwable) {
        if (throwable instanceof Error error) {
            throw error;
        }

        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new IllegalStateException(throwable);
    }
}
