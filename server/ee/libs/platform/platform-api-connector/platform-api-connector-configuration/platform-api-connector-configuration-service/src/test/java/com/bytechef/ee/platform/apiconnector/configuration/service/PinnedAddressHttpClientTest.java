/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.apiconnector.configuration.service.PinnedAddressHttpClient.Response;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocketFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class PinnedAddressHttpClientTest {

    private static final int MAX_BODY_BYTES = 1024;

    private final AtomicReference<String> receivedHostHeader = new AtomicReference<>();
    private final AtomicReference<String> receivedRequestUri = new AtomicReference<>();

    private HttpServer httpServer;
    private PinnedAddressHttpClient pinnedAddressHttpClient;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/fixed", exchange -> {
            recordRequest(exchange.getRequestHeaders()
                .getFirst("Host"), exchange.getRequestURI());

            byte[] body = "<p>fixed</p>".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "text/html; charset=UTF-8");

            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        httpServer.createContext("/chunked", exchange -> {
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write("<p>first ".getBytes(StandardCharsets.UTF_8));

                outputStream.flush();

                outputStream.write("second</p>".getBytes(StandardCharsets.UTF_8));
            }
        });
        httpServer.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders()
                .add("Location", "/fixed");

            exchange.sendResponseHeaders(302, -1);

            exchange.close();
        });
        httpServer.createContext("/large", exchange -> {
            byte[] body = new byte[MAX_BODY_BYTES + 1];

            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        httpServer.start();

        pinnedAddressHttpClient = new PinnedAddressHttpClient(
            Duration.ofSeconds(5), Duration.ofSeconds(5), MAX_BODY_BYTES,
            (SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testGetConnectsToThePinnedAddressAndSendsTheOriginalHost() throws IOException {
        int port = getPort();

        Response response = pinnedAddressHttpClient.get(
            URI.create("http://docs.example.com:" + port + "/fixed?page=1"), List.of(InetAddress.getLoopbackAddress()),
            Map.of("User-Agent", "test"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("<p>fixed</p>");
        assertThat(receivedHostHeader.get()).isEqualTo("docs.example.com:" + port);
        assertThat(receivedRequestUri.get()).isEqualTo("/fixed?page=1");
    }

    @Test
    void testGetDecodesChunkedBody() throws IOException {
        Response response = pinnedAddressHttpClient.get(
            URI.create("http://docs.example.com:" + getPort() + "/chunked"), List.of(InetAddress.getLoopbackAddress()),
            Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("<p>first second</p>");
    }

    @Test
    void testGetReturnsRedirectLocationWithoutFollowingIt() throws IOException {
        Response response = pinnedAddressHttpClient.get(
            URI.create("http://docs.example.com:" + getPort() + "/redirect"),
            List.of(InetAddress.getLoopbackAddress()), Map.of());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.location()).isEqualTo("/fixed");
        assertThat(receivedRequestUri.get()).isNull();
    }

    @Test
    void testGetRejectsBodyLargerThanTheLimit() {
        assertThatThrownBy(() -> pinnedAddressHttpClient.get(
            URI.create("http://docs.example.com:" + getPort() + "/large"), List.of(InetAddress.getLoopbackAddress()),
            Map.of()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void testGetFallsBackToTheNextPinnedAddressWhenTheFirstCannotBeReached() throws IOException {
        InetAddress unreachableAddress = InetAddress.getByAddress(new byte[] {
            (byte) 192, 0, 2, 1
        });

        PinnedAddressHttpClient shortTimeoutPinnedAddressHttpClient = new PinnedAddressHttpClient(
            Duration.ofMillis(300), Duration.ofSeconds(5), MAX_BODY_BYTES,
            (SSLSocketFactory) SSLSocketFactory.getDefault());

        Response response = shortTimeoutPinnedAddressHttpClient.get(
            URI.create("http://docs.example.com:" + getPort() + "/fixed"),
            List.of(unreachableAddress, InetAddress.getLoopbackAddress()), Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private int getPort() {
        InetSocketAddress serverAddress = httpServer.getAddress();

        return serverAddress.getPort();
    }

    private void recordRequest(String hostHeader, URI requestUri) {
        receivedHostHeader.set(hostHeader);
        receivedRequestUri.set(requestUri.toString());
    }
}
