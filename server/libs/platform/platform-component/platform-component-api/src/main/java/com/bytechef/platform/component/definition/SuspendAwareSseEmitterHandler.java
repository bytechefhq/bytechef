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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class SuspendAwareSseEmitterHandler implements SseEmitterHandler {

    private final ActionContextAware actionContext;
    private final SseEmitterHandler delegate;
    private final AtomicReference<@Nullable Throwable> failureReference = new AtomicReference<>();
    private final Object lock = new Object();
    private @Nullable Consumer<ActionContext.Suspend> beforeSuspendConsumer;
    private boolean beforeSuspendInvoked;

    public SuspendAwareSseEmitterHandler(SseEmitterHandler delegate, ActionContextAware actionContext) {
        this.actionContext = actionContext;
        this.delegate = delegate;
    }

    public SuspendAwareSseEmitterHandler withBeforeSuspend(Consumer<ActionContext.Suspend> beforeSuspendConsumer) {
        synchronized (lock) {
            if (this.beforeSuspendConsumer != null) {
                throw new IllegalStateException("The before-suspend consumer is already set");
            }

            this.beforeSuspendConsumer = beforeSuspendConsumer;
        }

        return this;
    }

    private ActionContext.@Nullable Suspend resolveSuspend() {
        if (failureReference.get() != null) {
            return null;
        }

        ActionContext.Suspend suspend = actionContext.getSuspend();

        if (suspend == null) {
            return null;
        }

        synchronized (lock) {
            if (beforeSuspendConsumer != null && !beforeSuspendInvoked) {
                beforeSuspendConsumer.accept(suspend);

                beforeSuspendInvoked = true;
            }
        }

        return actionContext.getSuspend();
    }

    public ActionContext.@Nullable Suspend getSuspendOrThrow(long jobId) {
        Throwable failure = failureReference.get();

        if (failure != null) {
            throw new IllegalStateException(
                "The streamed action of job " + jobId + " failed: " + failure.getMessage(), failure);
        }

        return resolveSuspend();
    }

    @Override
    public void handle(SseEmitter sseEmitter) {
        sseEmitter.addTimeoutListener(
            () -> failureReference.compareAndSet(null, new TimeoutException("The SSE stream timed out")));

        delegate.handle(new FailureTrackingSseEmitter(sseEmitter));
    }

    private final class FailureTrackingSseEmitter implements SseEmitter {

        private final SseEmitter sseEmitter;

        private FailureTrackingSseEmitter(SseEmitter sseEmitter) {
            this.sseEmitter = sseEmitter;
        }

        @Override
        public void addTimeoutListener(Runnable timeoutListener) {
            sseEmitter.addTimeoutListener(timeoutListener);
        }

        @Override
        public void complete() {
            sseEmitter.complete();
        }

        @Override
        public void error(Throwable throwable) {
            failureReference.compareAndSet(null, throwable);

            sseEmitter.error(throwable);
        }

        @Override
        public void send(Object data) {
            sseEmitter.send(data);
        }
    }
}
