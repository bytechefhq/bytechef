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
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
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
 * Worker-level processor for WebSocketHandler output. Sends events to the message broker for dispatching to the
 * coordinator's SseStreamBridgeRegistry.
 *
 * @author Ivica Cardic
 */
public class WebSocketStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(WebSocketStreamTaskExecutionPostOutputProcessor.class);

    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI")
    public WebSocketStreamTaskExecutionPostOutputProcessor(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof WebSocketHandler webSocketHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");
        String tenantId = TenantContext.getCurrentTenantId();

        WebSocketEmitterAdapter emitter = new WebSocketEmitterAdapter();

        emitter.addOutboundListener(
            payload -> sendEvent(jobId, SseStreamEvent.EVENT_TYPE_DATA, payload, tenantId));

        CountDownLatch latch = new CountDownLatch(1);

        emitter.addCompletionListener(() -> {
            sendEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null, tenantId);

            latch.countDown();
        });

        emitter.addErrorListener(
            throwable -> sendEvent(jobId, SseStreamEvent.EVENT_TYPE_ERROR, throwable.getMessage(), tenantId));

        emitter.addTimeoutListener(() -> {
            sendEvent(jobId, SseStreamEvent.EVENT_TYPE_ERROR,
                "WebSocket stream timed out for job " + jobId, tenantId);

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

    private static class WebSocketEmitterAdapter implements WebSocketHandler.WebSocketEmitter {

        private final CopyOnWriteArrayList<Consumer<byte[]>> binaryMessageListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Object>> messageListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Object>> outboundListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<byte[]>> outboundBinaryListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();

        private volatile boolean completed;
        private @Nullable Long timeout;

        public WebSocketEmitterAdapter() {
            this.timeout = null;
        }

        public WebSocketEmitterAdapter(Long timeoutMillis) {
            this.timeout = timeoutMillis;
        }

        @Override
        public void addBinaryMessageListener(Consumer<byte[]> binaryMessageListener) {
            binaryMessageListeners.add(binaryMessageListener);
        }

        @Override
        public void addCloseListener(Runnable closeListener) {
            if (completed) {
                closeListener.run();
            } else {
                closeListeners.add(closeListener);
            }
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

        @Override
        public void addMessageListener(Consumer<Object> messageListener) {
            messageListeners.add(messageListener);
        }

        public void addOutboundBinaryListener(Consumer<byte[]> listener) {
            outboundBinaryListeners.add(listener);
        }

        public void addOutboundListener(Consumer<Object> listener) {
            outboundListeners.add(listener);
        }

        @Override
        public void addTimeoutListener(Runnable listener) {
            timeoutListeners.add(listener);
        }

        @Override
        public void complete() {
            if (completed) {
                return;
            }

            completed = true;

            for (Runnable listener : closeListeners) {
                listener.run();
            }

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
        public void send(Object data) {
            for (var listener : outboundListeners) {
                listener.accept(data);
            }
        }

        @Override
        public void sendBinary(byte[] data) {
            for (var listener : outboundBinaryListeners) {
                listener.accept(data);
            }
        }

        public void triggerTimeout() {
            for (Runnable listener : timeoutListeners) {
                listener.run();
            }
        }
    }
}
