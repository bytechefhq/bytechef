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
 * @author Ivica Cardic
 */
final class PolyglotGuestExecutor {

    private static final AtomicLong THREAD_COUNT = new AtomicLong();

    private static final long KEEP_ALIVE_SECONDS = 60;

    private final ExecutorService executorService;

    PolyglotGuestExecutor(int maxConcurrentExecutions) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            maxConcurrentExecutions, maxConcurrentExecutions, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), PolyglotGuestExecutor::newGuestThread);

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        this.executorService = threadPoolExecutor;
    }

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

    void shutdown() {
        executorService.shutdown();
    }

    private static Thread newGuestThread(Runnable runnable) {
        return Thread.ofPlatform()
            .name("polyglot-guest-" + THREAD_COUNT.incrementAndGet())
            .daemon(true)
            .unstarted(runnable);
    }

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
