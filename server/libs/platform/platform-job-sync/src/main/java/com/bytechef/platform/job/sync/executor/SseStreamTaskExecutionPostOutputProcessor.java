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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A private class that processes the output of a task execution post-processing phase. This implementation specifically
 * handles outputs of type {@code SseEmitter} and establishes an event-driven mechanism to manage Server-Sent Events
 * (SSE) streams. <br/>
 * This processor enables the integration of SSE functionality by creating an emitter and wiring it with listeners for
 * multiple event types, including: - Payload events: Deliver payloads to registered {@code SseStreamBridge} instances.
 * - Completion events: Notify when the stream is completed. - Error events: Handle errors occurring during stream
 * processing. - Timeout events: Handle timeout scenarios where the SSE stream does not complete within the designated
 * time. <br/>
 * The processing is job-specific, with each job identified by its unique {@code jobId}. The class interacts with
 * job-scoped SSE stream bridges stored in the enclosing {@code JobSyncExecutor}. <br/>
 * Key functionalities: - Listens for events on an SSE stream and delegates event handling to registered
 * {@code SseStreamBridge} instances. - Manages the lifecycle of the stream, including timeout and completion scenarios.
 * - Ensures gracefully shutting down resources and preventing interruptions. <br/>
 * Implements: {@code com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor}
 * <p>
 * Processing Flow: 1. Verifies if the task output is an instance of {@code SseEmitter}. 2. Creates and configures an
 * {@code Emitter}, binding it to relevant listeners. 3. Interacts with {@code SseStreamBridge} instances associated
 * with the job. 4. Waits for the emitter to complete or timeout using a {@code CountDownLatch}. 5. Handles timeouts,
 * interruptions, and other exception scenarios gracefully. <br/>
 * Return Value: - Returns {@code null} if the task output was successfully processed as an SSE stream. Otherwise, the
 * original task output is returned unchanged.
 *
 * @author Ivica Cardic
 */
class SseStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SseStreamTaskExecutionPostOutputProcessor.class);

    private final Cache<String, CopyOnWriteArrayList<com.bytechef.platform.job.sync.SseStreamBridge>> sseStreamBridges;

    SseStreamTaskExecutionPostOutputProcessor(
        Cache<String, CopyOnWriteArrayList<com.bytechef.platform.job.sync.SseStreamBridge>> sseStreamBridges) {
        this.sseStreamBridges = sseStreamBridges;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof ActionDefinition.SseEmitterHandler sseEmitterHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");

        String key = TenantCacheKeyUtils.getKey(jobId);

        SseEmitter emitter = new SseEmitter();

        emitter.addEventListener(payload -> {
            var sseStreamBridges = this.sseStreamBridges.getIfPresent(key);

            if (sseStreamBridges != null) {
                for (var sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onEvent(payload);
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }
        });

        CountDownLatch latch = new CountDownLatch(1);

        emitter.addCompletionListener(() -> {
            var sseStreamBridges = this.sseStreamBridges.getIfPresent(key);

            if (sseStreamBridges != null) {
                for (var sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onComplete();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            latch.countDown();
        });

        emitter.addErrorListener(throwable -> {
            var sseStreamBridges = this.sseStreamBridges.getIfPresent(key);

            if (sseStreamBridges != null) {
                for (var sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onError(throwable);
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }
        });

        emitter.addTimeoutListener(() -> {
            var sseStreamBridges = this.sseStreamBridges.getIfPresent(key);

            if (sseStreamBridges != null) {
                for (var sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onError(new TimeoutException("SSE stream timed out for job " + jobId));
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            latch.countDown();
        });

        sseEmitterHandler.handle(emitter);

        try {
            Long timeout = emitter.getTimeout();

            if (timeout == null || timeout < 0) {
                latch.await();
            } else {
                boolean finished = latch.await(timeout, TimeUnit.MILLISECONDS);

                if (!finished) {
                    emitter.triggerTimeout();
                }
            }
        } catch (InterruptedException ignored) {
            Thread thread = Thread.currentThread();

            thread.interrupt();
        }

        return null;
    }
}
