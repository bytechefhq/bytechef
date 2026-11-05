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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes WebSocketHandler output from task execution. Creates a WebSocketEmitter, wires it with listeners that
 * dispatch events to registered SseStreamBridge instances, and blocks until the WebSocket session completes or fails.
 *
 * @author Ivica Cardic
 */
class WebSocketStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger log =
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

        emitter.addOutboundListener(
            payload -> notifySseStreamBridges(key, sseStreamBridge -> sseStreamBridge.onEvent(payload)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean streamCompleted = new AtomicBoolean();
        AtomicBoolean streamFailed = new AtomicBoolean();

        emitter.addCompletionListener(() -> {
            streamCompleted.set(true);

            latch.countDown();
        });

        emitter.addErrorListener(throwable -> {
            streamFailed.set(true);

            notifySseStreamBridges(key, sseStreamBridge -> sseStreamBridge.onError(throwable));
        });

        emitter.addTimeoutListener(() -> {
            streamFailed.set(true);

            notifySseStreamBridges(
                key,
                sseStreamBridge -> sseStreamBridge.onError(
                    new TimeoutException("WebSocket stream timed out for job " + jobId)));

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

        if (streamCompleted.get() && !streamFailed.get()) {
            notifySseStreamBridges(key, SseStreamBridge::onComplete);
        }

        return null;
    }

    private void notifySseStreamBridges(String key, Consumer<SseStreamBridge> sseStreamBridgeConsumer) {
        var sseStreamBridges = this.sseStreamBridges.getIfPresent(key);

        if (sseStreamBridges == null) {
            return;
        }

        for (var sseStreamBridge : sseStreamBridges) {
            try {
                sseStreamBridgeConsumer.accept(sseStreamBridge);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }
}
