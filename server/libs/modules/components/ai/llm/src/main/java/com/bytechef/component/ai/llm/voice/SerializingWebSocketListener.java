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

package com.bytechef.component.ai.llm.voice;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
final class SerializingWebSocketListener implements WebSocket.Listener {

    private static final Logger log = LoggerFactory.getLogger(SerializingWebSocketListener.class);

    private final WebSocket.Listener delegate;
    private final AtomicReference<SerializedWebSocket> holder;

    SerializingWebSocketListener(WebSocket.Listener delegate, AtomicReference<SerializedWebSocket> holder) {
        this.delegate = delegate;
        this.holder = holder;
    }

    SerializedWebSocket serialized(WebSocket raw) {
        SerializedWebSocket existing = holder.get();

        if (existing != null) {
            warnIfDelegateChanged(existing, raw);

            return existing;
        }

        SerializedWebSocket created = new SerializedWebSocket(raw);

        if (holder.compareAndSet(null, created)) {
            return created;
        }

        SerializedWebSocket winner = holder.get();

        warnIfDelegateChanged(winner, raw);

        return winner;
    }

    private static void warnIfDelegateChanged(SerializedWebSocket serializedWebSocket, WebSocket raw) {
        if (serializedWebSocket.delegate() != raw) {
            log.warn(
                "A voice provider callback arrived with a raw WebSocket different from the one already serialized "
                    + "for this connection; continuing with the already-serialized instance. This should never happen.");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        delegate.onOpen(serialized(webSocket));
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        return delegate.onText(serialized(webSocket), data, last);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        return delegate.onBinary(serialized(webSocket), data, last);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        return delegate.onPing(serialized(webSocket), message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        return delegate.onPong(serialized(webSocket), message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        return delegate.onClose(serialized(webSocket), statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        delegate.onError(serialized(webSocket), error);
    }
}
