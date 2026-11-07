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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SerializedWebSocketTest {

    @Test
    void testRawDelegateThrowsWhenASendStartsWhileAPreviousSendIsPending() {
        ManualPendingWebSocket delegate = new ManualPendingWebSocket();

        delegate.sendText("first", true);

        assertThatThrownBy(() -> delegate.sendText("second", true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Send pending");
    }

    @Test
    void testSerializedWebSocketQueuesOverlappingSendsInsteadOfLettingThemOverlap() {
        ManualPendingWebSocket delegate = new ManualPendingWebSocket();
        SerializedWebSocket serializedWebSocket = new SerializedWebSocket(delegate);

        CompletableFuture<WebSocket> firstResult = serializedWebSocket.sendText("first", true);
        CompletableFuture<WebSocket> secondResult =
            serializedWebSocket.sendBinary(ByteBuffer.wrap(new byte[] {
                1
            }), true);

        assertThat(delegate.events()).containsExactly("text:first");
        assertThat(firstResult).isNotDone();
        assertThat(secondResult).isNotDone();

        delegate.completePending();

        assertThat(firstResult).isCompletedWithValueMatching(webSocket -> webSocket == serializedWebSocket);
        assertThat(delegate.events()).containsExactly("text:first", "binary");
        assertThat(secondResult).isNotDone();

        delegate.completePending();

        assertThat(secondResult).isCompletedWithValueMatching(webSocket -> webSocket == serializedWebSocket);
    }

    @Test
    void testSerializedWebSocketStartsTheNextSendAfterAPriorSendFailsExceptionally() {
        ManualPendingWebSocket delegate = new ManualPendingWebSocket();
        SerializedWebSocket serializedWebSocket = new SerializedWebSocket(delegate);

        CompletableFuture<WebSocket> firstResult = serializedWebSocket.sendText("boom", true);

        delegate.failPending(new IllegalStateException("network reset"));

        assertThatThrownBy(firstResult::join).hasCauseInstanceOf(IllegalStateException.class);

        CompletableFuture<WebSocket> secondResult = serializedWebSocket.sendText("after failure", true);

        assertThat(delegate.events()).containsExactly("text:boom", "text:after failure");

        delegate.completePending();

        assertThat(secondResult).isCompletedWithValueMatching(webSocket -> webSocket == serializedWebSocket);
    }

    private static final class ManualPendingWebSocket implements WebSocket {

        private final List<String> events = new CopyOnWriteArrayList<>();
        private CompletableFuture<WebSocket> pending;

        List<String> events() {
            return List.copyOf(events);
        }

        void completePending() {
            takePending().complete(this);
        }

        void failPending(Throwable throwable) {
            takePending().completeExceptionally(throwable);
        }

        private CompletableFuture<WebSocket> takePending() {
            CompletableFuture<WebSocket> future = pending;

            pending = null;

            return future;
        }

        private CompletableFuture<WebSocket> send(String label) {
            if (pending != null) {
                throw new IllegalStateException("Send pending");
            }

            events.add(label);
            pending = new CompletableFuture<>();

            return pending;
        }

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            return send("text:" + data);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return send("binary");
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return send("ping");
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return send("pong");
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return send("close");
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
