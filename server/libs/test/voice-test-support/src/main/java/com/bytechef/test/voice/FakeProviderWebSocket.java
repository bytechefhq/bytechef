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

package com.bytechef.test.voice;

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ivica Cardic
 */
public final class FakeProviderWebSocket implements WebSocket {

    private final Listener listener;
    private final List<String> sentTexts = new CopyOnWriteArrayList<>();
    private final List<byte[]> sentBinaries = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    private FakeProviderWebSocket(Listener listener) {
        this.listener = listener;
    }

    public List<String> sentTexts() {
        return List.copyOf(sentTexts);
    }

    public List<byte[]> sentBinaries() {
        return List.copyOf(sentBinaries);
    }

    public boolean closed() {
        return closed;
    }

    public void receiveText(String text) {
        listener.onText(this, text, true);
    }

    public void receiveBinary(byte[] bytes) {
        listener.onBinary(this, ByteBuffer.wrap(bytes), true);
    }

    public void receiveClose(int code, String reason) {
        listener.onClose(this, code, reason);
    }

    public void receiveError(Throwable throwable) {
        listener.onError(this, throwable);
    }

    @Override
    public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
        sentTexts.add(data.toString());

        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
        byte[] bytes = new byte[data.remaining()];

        data.get(bytes);
        sentBinaries.add(bytes);

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
        closed = true;

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
        return closed;
    }

    @Override
    public boolean isInputClosed() {
        return closed;
    }

    @Override
    public void abort() {
        closed = true;
    }

    public static final class Connector implements ProviderWebSocketConnector {

        private URI lastUri;
        private Map<String, String> lastHeaders;
        private FakeProviderWebSocket lastSocket;

        @Override
        public WebSocket connect(URI uri, Map<String, String> headers, Listener listener) {
            lastUri = uri;
            lastHeaders = Map.copyOf(headers);
            lastSocket = new FakeProviderWebSocket(listener);

            listener.onOpen(lastSocket);

            return lastSocket;
        }

        public URI lastUri() {
            return lastUri;
        }

        public Map<String, String> lastHeaders() {
            return lastHeaders;
        }

        public FakeProviderWebSocket lastSocket() {
            return lastSocket;
        }
    }
}
