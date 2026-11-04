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

package com.bytechef.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class PrivateAddressGuardedHttpClientTest {

    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();

    private HttpServer httpServer;
    private int port;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        port = httpServer.getAddress()
            .getPort();

        httpServer.createContext("/secret", httpExchange -> respond(httpExchange, 200, null, "internal secret"));
        httpServer.createContext("/public", httpExchange -> respond(httpExchange, 200, null, "public page"));
        httpServer.createContext(
            "/to-loopback-address",
            httpExchange -> respond(httpExchange, 302, "http://127.0.0.1:" + port + "/secret", ""));
        httpServer.createContext(
            "/to-allowed-host", httpExchange -> respond(httpExchange, 302, "/public", ""));
        httpServer.createContext(
            "/loop", httpExchange -> respond(httpExchange, 302, "/loop", ""));

        httpServer.start();
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testSendRefusesALoopbackAddress() {
        HttpClient httpClient = createHttpClient(Set.of());

        assertThatThrownBy(() -> get(httpClient, "http://127.0.0.1:" + port + "/secret"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("private or loopback address");

        assertThat(requestedPaths).isEmpty();
    }

    @Test
    void testSendFollowsARedirectWithinAnAllowedHost() throws Exception {
        HttpResponse<String> response = get(createHttpClient(Set.of("localhost")), url("/to-allowed-host"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("public page");
        assertThat(requestedPaths).containsExactly("/to-allowed-host", "/public");
    }

    @Test
    void testSendRefusesARedirectToAPrivateAddress() {
        HttpClient httpClient = createHttpClient(Set.of("localhost"));

        assertThatThrownBy(() -> get(httpClient, url("/to-loopback-address")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("private or loopback address");

        assertThat(requestedPaths).containsExactly("/to-loopback-address");
    }

    @Test
    void testSendAsyncRefusesARedirectToAPrivateAddress() {
        HttpClient httpClient = createHttpClient(Set.of("localhost"));

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url("/to-loopback-address")))
            .build();

        assertThatThrownBy(() -> httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class);

        assertThat(requestedPaths).doesNotContain("/secret");
    }

    @Test
    void testSendStopsAfterTooManyRedirects() {
        HttpClient httpClient = createHttpClient(Set.of("localhost"));

        assertThatThrownBy(() -> get(httpClient, url("/loop")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Too many redirects");
    }

    @Test
    void testReplaceHttpClientKeepsTheConnectTimeout() throws Exception {
        HttpClientOwner httpClientOwner = new HttpClientOwner();

        PrivateAddressGuardedHttpClient.replaceHttpClient(httpClientOwner, "httpClient", Set.of("localhost"));

        assertThat(httpClientOwner.httpClient).isInstanceOf(PrivateAddressGuardedHttpClient.class);
        assertThat(httpClientOwner.httpClient.connectTimeout()).contains(Duration.ofSeconds(7));

        assertThatThrownBy(() -> get(httpClientOwner.httpClient, url("/to-loopback-address")))
            .isInstanceOf(IOException.class);
    }

    @Test
    void testReplaceHttpClientFailsForAMissingField() {
        assertThatThrownBy(
            () -> PrivateAddressGuardedHttpClient.replaceHttpClient(new HttpClientOwner(), "client", Set.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    private static HttpClient createHttpClient(Set<String> allowedPrivateHosts) {
        return new PrivateAddressGuardedHttpClient(HttpClient.newBuilder(), allowedPrivateHosts);
    }

    private static HttpResponse<String> get(HttpClient httpClient, String url)
        throws IOException, InterruptedException {

        return httpClient.send(
            HttpRequest.newBuilder(URI.create(url))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private void respond(HttpExchange httpExchange, int statusCode, String location, String body)
        throws IOException {

        URI requestUri = httpExchange.getRequestURI();

        requestedPaths.add(requestUri.getPath());

        if (location != null) {
            httpExchange.getResponseHeaders()
                .add("Location", location);
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        httpExchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);

        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static final class HttpClientOwner {

        private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(7))
            .build();
    }
}
