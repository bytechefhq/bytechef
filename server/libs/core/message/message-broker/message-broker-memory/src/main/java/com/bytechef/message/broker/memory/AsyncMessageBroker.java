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

package com.bytechef.message.broker.memory;

import com.bytechef.message.Retryable;
import com.bytechef.message.route.MessageRoute;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.thread.Threading;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * An asynchronous implementation of a message broker for routing messages to subscribed listeners. This class extends
 * {@code AbstractMessageBroker}, providing a non-blocking approach to message delivery using an {@code Executor} for
 * task execution. The {@code AsyncMessageBroker} is designed to decouple the process of message sending and delivery by
 * executing receiver logic in separate threads managed by the provided executor.
 *
 * @author Ivica Cardic
 */
public class AsyncMessageBroker extends AbstractMessageBroker implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageBroker.class);

    /**
     * Backpressure threshold for ordered per-route executors. Large enough to absorb typical SSE streaming bursts (LLM
     * tokens arrive at 50-200/sec, HTTP writer drains within the same cadence) without pinning much memory per route;
     * small enough that a truly stuck receiver blocks the producer quickly instead of silently accumulating unbounded
     * backlog.
     */
    private static final int ORDERED_QUEUE_CAPACITY = 1024;

    private static final long SHUTDOWN_AWAIT_SECONDS = 5;

    private final ExecutorService executor;
    private final boolean virtualThreads;
    private final Map<MessageRoute, ExecutorService> orderedRouteExecutors = new ConcurrentHashMap<>();

    public AsyncMessageBroker(Environment environment) {
        this.virtualThreads = Threading.VIRTUAL.isActive(environment);

        if (virtualThreads) {
            executor = Executors.newVirtualThreadPerTaskExecutor();
        } else {
            executor = Executors.newCachedThreadPool();
        }
    }

    @Override
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void send(MessageRoute messageRoute, Object message) {
        Assert.notNull(messageRoute, "'messageRoute' must not be null");

        if (message instanceof Retryable retryable) {
            delay(retryable.getRetryDelayMillis());
        }

        List<Receiver> receivers = receiverMap.get(messageRoute);

        if (receivers == null || receivers.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No listeners subscribed for: " + messageRoute);
            }

            return;
        }

        ContextSnapshot contextSnapshot = ContextSnapshotFactory.builder()
            .build()
            .captureAll();

        Executor dispatchExecutor = messageRoute.isOrdered()
            ? orderedRouteExecutors.computeIfAbsent(messageRoute, this::createOrderedExecutor)
            : executor;

        for (Receiver receiver : Validate.notNull(receivers, "receivers")) {
            dispatchExecutor.execute(
                () -> {
                    try (ContextSnapshot.Scope scope = contextSnapshot.setThreadLocals()) {
                        receiver.receive(message);
                    }
                });
        }
    }

    private ExecutorService createOrderedExecutor(MessageRoute messageRoute) {
        ThreadFactory threadFactory = buildOrderedThreadFactory(messageRoute);

        // Single worker thread preserves FIFO; bounded queue caps memory; the blocking rejection
        // handler applies backpressure to the producer when the receiver can't keep up, instead of
        // dropping messages (which would break ordering semantics for SSE tokens).
        return new ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(ORDERED_QUEUE_CAPACITY),
            threadFactory,
            (task, runningExecutor) -> {
                if (runningExecutor.isShutdown()) {
                    throw new RejectedExecutionException(
                        "Ordered executor is shut down; cannot enqueue message");
                }

                try {
                    runningExecutor.getQueue()
                        .put(task);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    throw new RejectedExecutionException(
                        "Interrupted while waiting for ordered queue capacity", interruptedException);
                }
            });
    }

    @Override
    public void destroy() {
        shutdown();
    }

    public void shutdown() {
        executor.shutdown();

        for (ExecutorService routeExecutor : orderedRouteExecutors.values()) {
            routeExecutor.shutdown();
        }

        awaitTermination(executor);

        for (ExecutorService routeExecutor : orderedRouteExecutors.values()) {
            awaitTermination(routeExecutor);
        }

        orderedRouteExecutors.clear();
    }

    private void awaitTermination(ExecutorService executorService) {
        try {
            if (!executorService.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            executorService.shutdownNow();

            Thread.currentThread()
                .interrupt();
        }
    }

    private ThreadFactory buildOrderedThreadFactory(MessageRoute messageRoute) {
        AtomicInteger counter = new AtomicInteger();
        String prefix = "async-mb-ordered-" + messageRoute.getName() + "-";

        if (virtualThreads) {
            return runnable -> Thread.ofVirtual()
                .name(prefix + counter.incrementAndGet())
                .unstarted(runnable);
        }

        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());

            thread.setDaemon(true);

            return thread;
        };
    }

    private void delay(long value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }
    }
}
