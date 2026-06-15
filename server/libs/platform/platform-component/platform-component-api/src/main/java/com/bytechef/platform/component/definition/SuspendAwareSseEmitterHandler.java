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

import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;

/**
 * An {@link SseEmitterHandler} that also carries the {@link ActionContextAware} of the streaming action, so a
 * post-output processor can check {@code getSuspend()} after the stream has been fully consumed — the only point at
 * which a mid-stream tool suspend is observable.
 *
 * @author Ivica Cardic
 */
public final class SuspendAwareSseEmitterHandler implements SseEmitterHandler {

    private final SseEmitterHandler delegate;
    private final ActionContextAware actionContext;

    public SuspendAwareSseEmitterHandler(SseEmitterHandler delegate, ActionContextAware actionContext) {
        this.delegate = delegate;
        this.actionContext = actionContext;
    }

    public ActionContextAware getActionContext() {
        return actionContext;
    }

    @Override
    public void handle(SseEmitter sseEmitter) {
        delegate.handle(sseEmitter);
    }
}
