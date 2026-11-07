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

import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class WebSocketEmitterRegistry {

    private final ConcurrentMap<String, CompletableFuture<WebSocketEmitter>> emitters = new ConcurrentHashMap<>();

    public CompletableFuture<WebSocketEmitter> awaitEmitter(long sessionId, String elementName) {
        return emitters.computeIfAbsent(key(sessionId, elementName), k -> new CompletableFuture<>());
    }

    public void register(long sessionId, String elementName, WebSocketEmitter emitter) {
        emitters.computeIfAbsent(key(sessionId, elementName), k -> new CompletableFuture<>())
            .complete(emitter);
    }

    public void unregisterAll(long sessionId) {
        String prefix = sessionId + ":";

        emitters.keySet()
            .removeIf(k -> k.startsWith(prefix));
    }

    public Optional<WebSocketEmitter> get(long sessionId, String elementName) {
        CompletableFuture<WebSocketEmitter> future = emitters.get(key(sessionId, elementName));

        if (future == null || !future.isDone()) {
            return Optional.empty();
        }

        try {
            return Optional.of(future.get(0, TimeUnit.MILLISECONDS));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static String key(long sessionId, String elementName) {
        return sessionId + ":" + (elementName == null ? "" : elementName);
    }
}
