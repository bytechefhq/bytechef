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

package com.bytechef.platform.notification.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ivica Cardic
 */
final class LoopbackWebhookReceiver implements AutoCloseable {

    private final AtomicInteger receivedCount = new AtomicInteger();
    private final BlockingQueue<ReceivedRequest> receivedRequests = new LinkedBlockingQueue<>();
    private volatile CountDownLatch holdResponse;
    private final HttpServer httpServer;
    private volatile int responseStatus = 200;

    LoopbackWebhookReceiver() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        httpServer.createContext("/", this::handle);
        httpServer.start();
    }

    ReceivedRequest awaitRequest() throws InterruptedException {
        ReceivedRequest receivedRequest = receivedRequests.poll(10, TimeUnit.SECONDS);

        if (receivedRequest == null) {
            throw new IllegalStateException("No request reached the receiver");
        }

        return receivedRequest;
    }

    @Override
    public void close() {
        CountDownLatch latch = holdResponse;

        if (latch != null) {
            latch.countDown();
        }

        httpServer.stop(0);
    }

    int getReceivedCount() {
        return receivedCount.get();
    }

    String getUrl(String path) {
        return "http://localhost:" + httpServer.getAddress()
            .getPort() + path;
    }

    void holdResponses(CountDownLatch latch) {
        this.holdResponse = latch;
    }

    void respondWith(int status) {
        this.responseStatus = status;
    }

    private void handle(HttpExchange httpExchange) throws IOException {
        byte[] body = httpExchange.getRequestBody()
            .readAllBytes();

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Headers requestHeaders = httpExchange.getRequestHeaders();

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            headers.put(entry.getKey(), entry.getValue()
                .getFirst());
        }

        receivedCount.incrementAndGet();

        receivedRequests.add(
            new ReceivedRequest(
                httpExchange.getRequestMethod(), httpExchange.getRequestURI()
                    .getPath(),
                headers, new String(body, StandardCharsets.UTF_8)));

        CountDownLatch latch = holdResponse;

        if (latch != null) {
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();
            }
        }

        httpExchange.sendResponseHeaders(responseStatus, -1);
        httpExchange.close();
    }

    record ReceivedRequest(String method, String path, Map<String, String> headers, String body) {
    }
}
