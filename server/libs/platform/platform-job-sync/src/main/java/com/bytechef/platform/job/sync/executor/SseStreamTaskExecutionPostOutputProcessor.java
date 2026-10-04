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
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
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
 * Streams an action's {@link ActionDefinition.SseEmitterHandler} output to the {@code SseStreamBridge}s registered for
 * the job, and waits until the stream completes, fails or times out.
 *
 * <p>
 * Returns the output unchanged when it is not an {@code SseEmitterHandler}. Otherwise, for a
 * {@link SuspendAwareSseEmitterHandler} it returns the suspend the stream recorded (or {@code null}) and throws when
 * the stream failed or timed out, so the task fails; for any other handler it returns {@code null}. It must run before
 * the suspend post-output processor, which persists the returned suspend.
 *
 * @author Ivica Cardic
 */
class SseStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger log = LoggerFactory.getLogger(SseStreamTaskExecutionPostOutputProcessor.class);

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
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
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
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
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
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
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
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
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

        if (output instanceof SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler) {
            return suspendAwareSseEmitterHandler.getSuspendOrThrow(jobId);
        }

        return null;
    }
}
