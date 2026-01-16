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

import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an implementation of the {@code SseEmitter.Emitter} interface for managing server-sent events (SSE) by
 * allowing data emission, and handling lifecycle events such as completion, errors, and timeouts. <br/>
 * This implementation utilizes thread-safe collections to manage listeners and maintains the emitter's state, ensuring
 * safe concurrent access in multi-threaded environments. The emitter supports: - Registering listeners for events,
 * completion, errors, and timeouts. - Emitting data to registered listeners. - Handling completion, error, or timeout
 * conditions, triggering corresponding listeners.
 *
 * @author Ivica Cardic
 */
class SseEmitter implements SseEmitterHandler.SseEmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseEmitter.class);

    private final CopyOnWriteArrayList<Consumer<Object>> eventListeners = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();
    private final @Nullable Long timeout;

    private volatile boolean completed;
    private volatile boolean timedOut;
    private volatile boolean errored;
    private volatile @Nullable Throwable lastError;

    public SseEmitter() {
        this.timeout = null;
    }

    public SseEmitter(Long timeoutMillis) {
        this.timeout = timeoutMillis;
    }

    public void addEventListener(Consumer<Object> consumer) {
        eventListeners.add(consumer);
    }

    public void addCompletionListener(Runnable callback) {
        if (completed) {
            try {
                callback.run();
            } catch (Exception exception) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
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
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
                }
            }
        } else {
            errorListeners.add(consumer);
        }
    }

    public void addTimeoutListener(Runnable listener) {
        if (timedOut) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
                }
            }
        } else {
            timeoutListeners.add(listener);
        }
    }

    @Override
    public void complete() {
        if (completed) {
            return;
        }

        completed = true;

        for (Runnable listener : completionListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
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
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
                }
            }
        }

        complete();
    }

    public @Nullable Long getTimeout() {
        return timeout;
    }

    @Override
    public void send(Object object) {
        for (var listener : eventListeners) {
            try {
                listener.accept(object);
            } catch (Exception exception) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    public void triggerTimeout() {
        timedOut = true;

        for (Runnable listener : timeoutListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(exception.getMessage(), exception);
                }
            }
        }
    }

}
