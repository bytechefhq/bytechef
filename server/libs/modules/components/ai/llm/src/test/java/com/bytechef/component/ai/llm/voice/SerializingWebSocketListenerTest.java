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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SerializingWebSocketListenerTest {

    @Test
    void testConnectPathBeforeAnyCallbackStillSharesOneInstance() {
        RawWebSocketStub rawWebSocket = new RawWebSocketStub();
        RecordingListener delegate = new RecordingListener();
        AtomicReference<SerializedWebSocket> holder = new AtomicReference<>();
        SerializingWebSocketListener serializingWebSocketListener =
            new SerializingWebSocketListener(delegate, holder);

        SerializedWebSocket connectReturnValue = serializingWebSocketListener.serialized(rawWebSocket);

        assertThat(connectReturnValue).isNotNull();

        serializingWebSocketListener.onOpen(rawWebSocket);
        serializingWebSocketListener.onText(rawWebSocket, "hello", true);
        serializingWebSocketListener.onClose(rawWebSocket, 1000, "done");

        assertThat(delegate.webSocketsSeen()).hasSize(3);
        assertThat(delegate.webSocketsSeen()).allSatisfy(
            webSocket -> assertThat(webSocket).isSameAs(connectReturnValue));
    }

    @Test
    void testCallbacksBeforeTheConnectPathStillShareOneInstance() {
        RawWebSocketStub rawWebSocket = new RawWebSocketStub();
        RecordingListener delegate = new RecordingListener();
        AtomicReference<SerializedWebSocket> holder = new AtomicReference<>();
        SerializingWebSocketListener serializingWebSocketListener =
            new SerializingWebSocketListener(delegate, holder);

        serializingWebSocketListener.onOpen(rawWebSocket);
        serializingWebSocketListener.onText(rawWebSocket, "hello", true);
        serializingWebSocketListener.onBinary(rawWebSocket, ByteBuffer.wrap(new byte[] {
            1
        }), true);
        serializingWebSocketListener.onPing(rawWebSocket, ByteBuffer.wrap(new byte[] {
            2
        }));
        serializingWebSocketListener.onPong(rawWebSocket, ByteBuffer.wrap(new byte[] {
            3
        }));
        serializingWebSocketListener.onClose(rawWebSocket, 1000, "done");
        serializingWebSocketListener.onError(rawWebSocket, new IllegalStateException("boom"));

        SerializedWebSocket connectReturnValue = serializingWebSocketListener.serialized(rawWebSocket);

        assertThat(connectReturnValue).isNotNull();
        assertThat(delegate.webSocketsSeen()).hasSize(7);
        assertThat(delegate.webSocketsSeen()).allSatisfy(
            webSocket -> assertThat(webSocket).isSameAs(connectReturnValue));
        assertThat(delegate.webSocketsSeen()).noneMatch(webSocket -> webSocket == rawWebSocket);
    }

    @Test
    void testConcurrentSerializedCallsAllReturnTheSameInstance() throws Exception {
        RawWebSocketStub rawWebSocket = new RawWebSocketStub();
        RecordingListener delegate = new RecordingListener();
        AtomicReference<SerializedWebSocket> holder = new AtomicReference<>();
        SerializingWebSocketListener serializingWebSocketListener =
            new SerializingWebSocketListener(delegate, holder);

        int threadCount = 16;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<SerializedWebSocket> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread.startVirtualThread(() -> {
                readyLatch.countDown();

                try {
                    startLatch.await();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    return;
                }

                results.add(serializingWebSocketListener.serialized(rawWebSocket));

                doneLatch.countDown();
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        assertThat(results).hasSize(threadCount);
        assertThat(results).allSatisfy(webSocket -> assertThat(webSocket).isSameAs(results.getFirst()));
    }

    private static final class RecordingListener implements WebSocket.Listener {

        private final List<WebSocket> webSocketsSeen = new ArrayList<>();

        List<WebSocket> webSocketsSeen() {
            return List.copyOf(webSocketsSeen);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocketsSeen.add(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            webSocketsSeen.add(webSocket);

            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocketsSeen.add(webSocket);

            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocketsSeen.add(webSocket);

            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocketsSeen.add(webSocket);

            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            webSocketsSeen.add(webSocket);

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            webSocketsSeen.add(webSocket);
        }
    }

    private static final class RawWebSocketStub implements WebSocket {

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return false;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {
        }
    }
}
