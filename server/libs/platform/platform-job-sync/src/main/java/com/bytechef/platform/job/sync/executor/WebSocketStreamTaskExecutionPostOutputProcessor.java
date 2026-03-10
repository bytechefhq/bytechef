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
import com.bytechef.platform.job.sync.SseStreamBridge;
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
 * Processes WebSocketHandler output from task execution. Creates a WebSocketEmitter, wires it with listeners that
 * dispatch events to registered SseStreamBridge instances, and blocks until the WebSocket session completes or times
 * out.
 *
 * @author Ivica Cardic
 */
class WebSocketStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(WebSocketStreamTaskExecutionPostOutputProcessor.class);

    private final Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges;

    WebSocketStreamTaskExecutionPostOutputProcessor(
        Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges) {

        this.sseStreamBridges = sseStreamBridges;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof ActionDefinition.WebSocketHandler webSocketHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");

        String key = TenantCacheKeyUtils.getKey(jobId);

        WebSocketEmitter emitter = new WebSocketEmitter();

        emitter.addOutboundListener(payload -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onEvent(payload);
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
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onComplete();
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
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onError(throwable);
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }
        });

        emitter.addTimeoutListener(() -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onError(new TimeoutException("WebSocket stream timed out for job " + jobId));
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            latch.countDown();
        });

        webSocketHandler.handle(emitter);

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
