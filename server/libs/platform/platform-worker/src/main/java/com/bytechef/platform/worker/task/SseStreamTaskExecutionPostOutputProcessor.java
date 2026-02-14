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

package com.bytechef.platform.worker.task;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SseStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SseStreamTaskExecutionPostOutputProcessor.class);

    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI")
    public SseStreamTaskExecutionPostOutputProcessor(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof ActionDefinition.SseEmitterHandler sseEmitterHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");
        String tenantId = TenantContext.getCurrentTenantId();

        ActionDefinition.SseEmitterHandler.SseEmitter emitter = createSseEmitter(jobId, tenantId);
        CountDownLatch latch = new CountDownLatch(1);

        addListeners(emitter, latch, jobId, tenantId);

        sseEmitterHandler.handle(emitter);

        awaitCompletion(emitter, latch);

        return null;
    }

    private void addListeners(
        ActionDefinition.SseEmitterHandler.SseEmitter emitter, CountDownLatch latch, long jobId, String tenantId) {

        if (emitter instanceof SseEmitterAdapter sseEmitterAdapter) {
            sseEmitterAdapter.addCompletionListener(() -> {
                sendEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null, tenantId);

                latch.countDown();
            });

            sseEmitterAdapter.addErrorListener(throwable -> {
                sendEvent(jobId, SseStreamEvent.EVENT_TYPE_ERROR, throwable.getMessage(), tenantId);
            });

            sseEmitterAdapter.addTimeoutListener(() -> {
                sendEvent(
                    jobId, SseStreamEvent.EVENT_TYPE_ERROR, "SSE stream timed out for job " + jobId, tenantId);

                latch.countDown();
            });
        }
    }

    private void awaitCompletion(ActionDefinition.SseEmitterHandler.SseEmitter emitter, CountDownLatch latch) {

        try {
            Long timeout = null;

            if (emitter instanceof SseEmitterAdapter sseEmitterAdapter) {
                timeout = sseEmitterAdapter.getTimeout();
            }

            if (timeout == null || timeout < 0) {
                latch.await();
            } else {
                boolean finished = latch.await(timeout, TimeUnit.MILLISECONDS);

                if (!finished && emitter instanceof SseEmitterAdapter sseEmitterAdapter) {
                    sseEmitterAdapter.triggerTimeout();
                }
            }
        } catch (InterruptedException ignored) {
            Thread thread = Thread.currentThread();

            thread.interrupt();
        }
    }

    private ActionDefinition.SseEmitterHandler.SseEmitter createSseEmitter(long jobId, String tenantId) {
        SseEmitterAdapter emitter = new SseEmitterAdapter();

        emitter.addEventListener(
            payload -> sendEvent(jobId, SseStreamEvent.EVENT_TYPE_DATA, payload, tenantId));

        return emitter;
    }

    private void sendEvent(long jobId, String eventType, @Nullable Object payload, String tenantId) {
        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(jobId, eventType, payload);

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, tenantId);

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }

    private static class SseEmitterAdapter implements ActionDefinition.SseEmitterHandler.SseEmitter {

        private final CopyOnWriteArrayList<Consumer<Object>> eventListeners = new CopyOnWriteArrayList<>();

        private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();

        private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();

        private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();

        private volatile boolean completed;
        private @Nullable Long timeout;

        public SseEmitterAdapter() {
            this.timeout = null;
        }

        public SseEmitterAdapter(Long timeoutMillis) {
            this.timeout = timeoutMillis;
        }

        public void addEventListener(Consumer<Object> consumer) {
            eventListeners.add(consumer);
        }

        public void addCompletionListener(Runnable callback) {
            if (completed) {
                callback.run();
            } else {
                completionListeners.add(callback);
            }
        }

        public void addErrorListener(Consumer<Throwable> consumer) {
            errorListeners.add(consumer);
        }

        public void addTimeoutListener(Runnable listener) {
            timeoutListeners.add(listener);
        }

        @Override
        public void complete() {
            if (completed) {
                return;
            }

            completed = true;

            for (Runnable listener : completionListeners) {
                listener.run();
            }
        }

        @Override
        public void error(Throwable throwable) {
            if (completed) {
                return;
            }

            for (var listener : errorListeners) {
                listener.accept(throwable);
            }

            complete();
        }

        public @Nullable Long getTimeout() {
            return timeout;
        }

        @Override
        public void send(Object object) {
            for (var listener : eventListeners) {
                listener.accept(object);
            }
        }

        public void triggerTimeout() {
            for (Runnable listener : timeoutListeners) {
                listener.run();
            }
        }
    }
}
