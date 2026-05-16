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

package com.bytechef.platform.webhook.voice;

import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe implementation of {@link WebSocketHandler.WebSocketEmitter} for managing bidirectional WebSocket
 * communication. Mirrors the {@link SseEmitter} pattern with additional support for incoming message listeners and
 * binary data.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class WebSocketEmitter implements WebSocketHandler.WebSocketEmitter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEmitter.class);

    private final CopyOnWriteArrayList<Consumer<byte[]>> binaryMessageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Object>> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Object>> outboundListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<byte[]>> outboundBinaryListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> outboundTurnCancelListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> turnCancelListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();
    private final @Nullable Long timeout;

    private final AtomicReference<@Nullable String> lastCancelledTurnId = new AtomicReference<>();

    private volatile boolean completed;
    private volatile boolean errored;
    private volatile @Nullable Throwable lastError;

    public WebSocketEmitter() {
        this.timeout = null;
    }

    public WebSocketEmitter(Long timeoutMillis) {
        this.timeout = timeoutMillis;
    }

    @Override
    public void addBinaryMessageListener(Consumer<byte[]> binaryMessageListener) {
        binaryMessageListeners.add(binaryMessageListener);
    }

    @Override
    public void addCloseListener(Runnable closeListener) {
        if (completed) {
            try {
                closeListener.run();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        } else {
            closeListeners.add(closeListener);
        }
    }

    public void addCompletionListener(Runnable callback) {
        if (completed) {
            try {
                callback.run();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        } else {
            completionListeners.add(callback);
        }
    }

    public void addErrorListener(Consumer<Throwable> consumer) {
        if (errored) {
            try {
                consumer.accept(Objects.requireNonNull(lastError));
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        } else {
            errorListeners.add(consumer);
        }
    }

    @Override
    public void addMessageListener(Consumer<Object> messageListener) {
        messageListeners.add(messageListener);
    }

    /**
     * Registers a listener for outbound binary data. Called by the platform to intercept binary data sent via
     * {@link #sendBinary(byte[])}.
     */
    public void addOutboundBinaryListener(Consumer<byte[]> listener) {
        outboundBinaryListeners.add(listener);
    }

    /**
     * Registers a listener for outbound data. Called by the platform to intercept data sent via {@link #send(Object)}.
     */
    public void addOutboundListener(Consumer<Object> listener) {
        outboundListeners.add(listener);
    }

    @Override
    public void addTimeoutListener(Runnable listener) {
        if (completed) {
            return;
        }

        timeoutListeners.add(listener);
    }

    @Override
    public void addTurnCancelListener(Consumer<String> turnCancelListener) {
        turnCancelListeners.add(turnCancelListener);
    }

    /**
     * Registers a listener for OUTBOUND turn-cancel propagation. The platform's WS handler chain wiring uses this to
     * forward {@code cancelTurn} across emitter boundaries (so a cancel fired on one chain node propagates to the next
     * node and back to the WS session).
     */
    public void addOutboundTurnCancelListener(Consumer<String> listener) {
        outboundTurnCancelListeners.add(listener);
    }

    @Override
    public void complete() {
        if (completed) {
            return;
        }

        completed = true;

        for (Runnable listener : closeListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }

        for (Runnable listener : completionListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    /**
     * Dispatches an incoming binary message to all registered binary message listeners.
     */
    public void dispatchBinaryMessage(byte[] data) {
        for (Consumer<byte[]> listener : binaryMessageListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    /**
     * Dispatches an incoming text message to all registered message listeners.
     */
    public void dispatchMessage(Object message) {
        for (Consumer<Object> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    @Override
    public void error(Throwable throwable) {
        if (completed) {
            return;
        }

        errored = true;
        lastError = throwable;

        for (var listener : errorListeners) {
            try {
                listener.accept(throwable);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }

        complete();
    }

    public @Nullable Long getTimeout() {
        return timeout;
    }

    @Override
    public void send(Object data) {
        for (var listener : outboundListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    @Override
    public void sendBinary(byte[] data) {
        for (var listener : outboundBinaryListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    @Override
    public void cancelTurn(String turnId) {
        // A cancel is broadcast to every other stage in the pipeline, and each of those broadcasts it back, so a
        // turn that has already been cancelled here must stop dead: without this guard a pipeline of two or more
        // stages ping-pongs the same turn until the stack overflows. Re-cancelling one turn is meaningless anyway.
        if (Objects.equals(lastCancelledTurnId.getAndSet(turnId), turnId)) {
            return;
        }

        // First fire inbound listeners (the component running on this emitter, e.g. the streaming agent,
        // aborts its in-flight work). Then fire outbound listeners (the platform chain wiring propagates
        // the cancel to adjacent emitters and the WS session).
        for (var listener : turnCancelListeners) {
            try {
                listener.accept(turnId);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }

        for (var listener : outboundTurnCancelListeners) {
            try {
                listener.accept(turnId);
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    public void triggerTimeout() {
        for (Runnable listener : timeoutListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }
}
