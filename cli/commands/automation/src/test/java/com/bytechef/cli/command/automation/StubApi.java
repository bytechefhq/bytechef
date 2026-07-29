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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A minimal in-process HTTP stub that records the request it received and replies with a fixed status and body, so
 * command tests can assert on the outgoing request without a live server.
 *
 * @author Ivica Cardic
 */
final class StubApi implements AutoCloseable {

    private final HttpServer server;

    private String lastPath;
    private String lastAuthorization;
    private String lastEnvironment;

    private StubApi(HttpServer server) {
        this.server = server;
    }

    static StubApi start(int status, String jsonBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

        StubApi stub = new StubApi(server);

        server.createContext("/", exchange -> {
            stub.lastPath = exchange.getRequestURI()
                .toString();
            stub.lastAuthorization = exchange.getRequestHeaders()
                .getFirst("Authorization");
            stub.lastEnvironment = exchange.getRequestHeaders()
                .getFirst("X-Environment");

            byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);

            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });

        server.start();

        return stub;
    }

    String host() {
        return "http://localhost:" + server.getAddress()
            .getPort();
    }

    String lastPath() {
        return lastPath;
    }

    String lastAuthorization() {
        return lastAuthorization;
    }

    String lastEnvironment() {
        return lastEnvironment;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
