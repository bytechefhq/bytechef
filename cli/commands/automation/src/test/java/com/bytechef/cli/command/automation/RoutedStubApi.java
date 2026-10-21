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

package com.bytechef.cli.command.automation;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An in-process HTTP stub that answers each {@code METHOD /path} with its own status and body and records every
 * request, so a command that makes several calls can be driven end to end. A request without a route gets a 404 whose
 * error body names it.
 *
 * @author Ivica Cardic
 */
final class RoutedStubApi implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Response> routes;
    private final Consumer<Request> onRequest;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    private RoutedStubApi(HttpServer server, Map<String, Response> routes, Consumer<Request> onRequest) {
        this.server = server;
        this.routes = Map.copyOf(routes);
        this.onRequest = onRequest;
    }

    static RoutedStubApi start(Map<String, Response> routes) throws IOException {
        return start(routes, request -> {});
    }

    /**
     * Like {@link #start(Map)}, and runs {@code onRequest} for each request before answering it, so a test can change
     * the file system while the command waits for the reply.
     */
    static RoutedStubApi start(Map<String, Response> routes, Consumer<Request> onRequest) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

        RoutedStubApi stub = new RoutedStubApi(server, routes, onRequest);

        server.createContext("/", stub::handle);
        server.start();

        return stub;
    }

    static Response response(int status, String body) {
        return new Response(status, body);
    }

    String host() {
        InetSocketAddress address = server.getAddress();

        return "http://localhost:" + address.getPort();
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    Optional<Request> lastRequest(String method, String path) {
        Request lastRequest = null;

        for (Request request : requests) {
            if (request.method()
                .equals(method) &&
                request.path()
                    .equals(path)) {

                lastRequest = request;
            }
        }

        return Optional.ofNullable(lastRequest);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        Headers requestHeaders = exchange.getRequestHeaders();
        String method = exchange.getRequestMethod();
        String path = requestUri.getPath();
        String requestBody;

        try (InputStream inputStream = exchange.getRequestBody()) {
            requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        Request request = new Request(
            method, path, requestUri.getRawQuery(), requestBody, requestHeaders.getFirst("Authorization"),
            requestHeaders.getFirst("X-Environment"));

        requests.add(request);

        onRequest.accept(request);

        Response response = routes.getOrDefault(
            method + " " + path,
            new Response(404, "{\"code\":\"STUB_NOT_FOUND\",\"message\":\"No stub for " + method + " " + path + "\"}"));
        byte[] responseBody = response.body()
            .getBytes(StandardCharsets.UTF_8);
        Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.add("Content-Type", "application/json");

        exchange.sendResponseHeaders(response.status(), responseBody.length == 0 ? -1 : responseBody.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    record Request(String method, String path, String query, String body, String authorization, String environment) {
    }

    record Response(int status, String body) {
    }
}
