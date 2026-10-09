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
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
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
 * Streams an action's {@link ActionDefinition.SseEmitterHandler} output to the {@code SseStreamBridge}s registered for
 * the job, and waits until the stream completes or fails. The emitter has no timeout, so the wait is unbounded. Events
 * sent while no bridge is registered are dropped, except an {@code ask_user_question} event: its send fails, so the
 * asking tool does not wait for answers nobody can give.
 *
 * <p>
 * Returns the output unchanged when it is not an {@code SseEmitterHandler}. Otherwise, for a
 * {@link SuspendAwareSseEmitterHandler} it returns the suspend the stream recorded (or {@code null}) and throws when
 * the stream failed, so the task fails; for any other handler it returns {@code null}. It must run before the suspend
 * post-output processor, which needs the returned suspend as its input to record the job resume id.
 *
 * @author Ivica Cardic
 */
class SseStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger log = LoggerFactory.getLogger(SseStreamTaskExecutionPostOutputProcessor.class);

    private final Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges;

    SseStreamTaskExecutionPostOutputProcessor(
        Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges) {
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

            if (sseStreamBridges == null || sseStreamBridges.isEmpty()) {
                if (isAskUserQuestionEvent(payload)) {
                    throw new IllegalStateException(
                        "No SSE connection is registered for job " + jobId + " to receive the '" +
                            AiAgentSseEventType.ASK_USER_QUESTION + "' event");
                }

                return;
            }

            boolean delivered = false;

            for (var sseStreamBridge : sseStreamBridges) {
                try {
                    sseStreamBridge.onEvent(payload);

                    delivered = true;
                } catch (Exception exception) {
                    if (log.isTraceEnabled()) {
                        log.trace(exception.getMessage(), exception);
                    }
                }
            }

            if (!delivered && isAskUserQuestionEvent(payload)) {
                throw new IllegalStateException(
                    "No SSE connection of job " + jobId + " received the '" + AiAgentSseEventType.ASK_USER_QUESTION +
                        "' event");
            }
        });

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
                    new TimeoutException("SSE stream timed out for job " + jobId)));

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

        Suspend suspend = null;

        if (output instanceof SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler) {
            try {
                suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(jobId);
            } catch (RuntimeException exception) {
                if (!streamFailed.get()) {
                    notifySseStreamBridges(key, sseStreamBridge -> sseStreamBridge.onError(exception));
                }

                throw exception;
            }
        }

        if (suspend != null) {
            notifySseStreamBridges(key, SseStreamBridge::onSuspend);

            return suspend;
        }

        if (!streamCompleted.get() || streamFailed.get()) {
            return null;
        }

        notifySseStreamBridges(key, SseStreamBridge::onComplete);

        return emitter.getOutput();
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

    private static boolean isAskUserQuestionEvent(@Nullable Object payload) {
        return payload instanceof Map<?, ?> map &&
            AiAgentSseEventType.ASK_USER_QUESTION.equals(map.get(AiAgentSseEventType.EVENT_TYPE));
    }
}
