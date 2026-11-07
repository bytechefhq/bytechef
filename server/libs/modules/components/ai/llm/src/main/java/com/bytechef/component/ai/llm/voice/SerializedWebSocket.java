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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public final class SerializedWebSocket implements WebSocket {

    private final WebSocket delegate;
    private final AtomicReference<CompletableFuture<Void>> queue =
        new AtomicReference<>(CompletableFuture.completedFuture(null));

    public SerializedWebSocket(WebSocket delegate) {
        this.delegate = delegate;
    }

    WebSocket delegate() {
        return delegate;
    }

    @Override
    public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
        return chain(() -> delegate.sendText(data, last));
    }

    @Override
    public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
        return chain(() -> delegate.sendBinary(data, last));
    }

    @Override
    public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
        return chain(() -> delegate.sendPing(message));
    }

    @Override
    public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
        return chain(() -> delegate.sendPong(message));
    }

    @Override
    public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
        return chain(() -> delegate.sendClose(statusCode, reason));
    }

    @Override
    public void request(long n) {
        delegate.request(n);
    }

    @Override
    public String getSubprotocol() {
        return delegate.getSubprotocol();
    }

    @Override
    public boolean isOutputClosed() {
        return delegate.isOutputClosed();
    }

    @Override
    public boolean isInputClosed() {
        return delegate.isInputClosed();
    }

    @Override
    public void abort() {
        delegate.abort();
    }

    private CompletableFuture<WebSocket> chain(Supplier<CompletableFuture<WebSocket>> operation) {
        CompletableFuture<WebSocket> result = new CompletableFuture<>();
        CompletableFuture<Void> nextSlot = new CompletableFuture<>();
        CompletableFuture<Void> previousSlot = queue.getAndSet(nextSlot);

        previousSlot.whenComplete((ignoredValue, ignoredThrowable) -> runOperation(operation, nextSlot, result));

        return result;
    }

    private void runOperation(
        Supplier<CompletableFuture<WebSocket>> operation, CompletableFuture<Void> nextSlot,
        CompletableFuture<WebSocket> result) {

        CompletableFuture<WebSocket> sendFuture;

        try {
            sendFuture = operation.get();
        } catch (RuntimeException runtimeException) {
            nextSlot.complete(null);
            result.completeExceptionally(runtimeException);

            return;
        }

        sendFuture.whenComplete((ignoredWebSocket, throwable) -> {
            nextSlot.complete(null);

            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                result.complete(this);
            }
        });
    }
}
