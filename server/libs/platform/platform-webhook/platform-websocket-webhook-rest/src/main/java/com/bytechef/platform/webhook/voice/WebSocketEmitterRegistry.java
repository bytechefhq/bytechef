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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Registry mapping each stage of a voice pipeline to its live {@link WebSocketEmitter}, keyed by
 * {@code (sessionId, stageName)}. Bridges {@link VoiceSessionEngine}, which creates the emitters, to the WS handlers
 * that own the caller's session ({@code WebhookWebSocketHandler} and {@code WorkflowTestWebSocketHandler}).
 *
 * <p>
 * Stage order in the pipeline IS the wiring — see {@link WebSocketTaskChain}. The first stage's inbound comes from the
 * caller's WS session; the last stage's outbound goes back to it.
 *
 * <p>
 * Both sides race naturally: the WS handler may install its bridges before the worker has created an emitter, or vice
 * versa. Each entry is therefore a {@link CompletableFuture} so both orders work uniformly.
 *
 * @author Ivica Cardic
 */
@Component
public class WebSocketEmitterRegistry {

    private final ConcurrentMap<String, CompletableFuture<WebSocketEmitter>> emitters = new ConcurrentHashMap<>();

    public CompletableFuture<WebSocketEmitter> awaitEmitter(long jobId, String taskName) {
        return emitters.computeIfAbsent(key(jobId, taskName), k -> new CompletableFuture<>());
    }

    public void register(long jobId, String taskName, WebSocketEmitter emitter) {
        emitters.computeIfAbsent(key(jobId, taskName), k -> new CompletableFuture<>())
            .complete(emitter);
    }

    public void unregisterAll(long jobId) {
        String prefix = jobId + ":";

        emitters.keySet()
            .removeIf(k -> k.startsWith(prefix));
    }

    public Optional<WebSocketEmitter> get(long jobId, String taskName) {
        CompletableFuture<WebSocketEmitter> future = emitters.get(key(jobId, taskName));

        if (future == null || !future.isDone()) {
            return Optional.empty();
        }

        try {
            return Optional.of(future.get(0, TimeUnit.MILLISECONDS));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static String key(long jobId, String taskName) {
        return jobId + ":" + (taskName == null ? "" : taskName);
    }
}
