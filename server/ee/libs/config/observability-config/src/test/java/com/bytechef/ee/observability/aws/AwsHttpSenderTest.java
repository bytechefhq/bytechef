/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Exports through a real {@link OtlpHttpLogRecordExporter} to a local HTTP server and verifies, with the AWS SDK signer
 * as an independent oracle, that the signature covers exactly the bytes and headers that arrived.
 *
 * @version ee
 *
 * @author Igor Beslic
 */
class AwsHttpSenderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T10:15:30Z"), ZoneOffset.UTC);
    private static final AwsSigV4Signer.Credentials CREDENTIALS = new AwsSigV4Signer.Credentials(
        "ASIAEXAMPLE", "secretExample", "sessionTokenExample");

    private final Queue<ReceivedRequest> receivedRequests = new ConcurrentLinkedQueue<>();
    private final Queue<Integer> responseStatusCodes = new ConcurrentLinkedQueue<>();

    private HttpServer httpServer;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/v1/logs", exchange -> {
            try (InputStream inputStream = exchange.getRequestBody()) {
                Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

                exchange.getRequestHeaders()
                    .forEach((name, values) -> headers.put(name, String.join(",", values)));

                receivedRequests.add(new ReceivedRequest(headers, inputStream.readAllBytes()));
            }

            Integer statusCode = responseStatusCodes.poll();

            exchange.sendResponseHeaders(statusCode == null ? 200 : statusCode, -1);
            exchange.close();
        });

        httpServer.start();
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testExportSignsTheBytesOnTheWire() {
        CompletableResultCode resultCode = export("none", RetryPolicy.getDefault());

        assertThat(resultCode.isSuccess()).isTrue();
        assertThat(receivedRequests).hasSize(1);

        ReceivedRequest receivedRequest = receivedRequests.remove();

        assertThat(receivedRequest.headers()).containsEntry("Content-Type", "application/x-protobuf")
            .containsEntry("X-Amz-Security-Token", "sessionTokenExample")
            .containsEntry("x-aws-log-group", "bytechef")
            .doesNotContainKey("Content-Encoding");
        assertThat(receivedRequest.body()).isNotEmpty();

        assertSignatureValid(receivedRequest);
    }

    @Test
    void testExportSignsCompressedPayload() {
        CompletableResultCode resultCode = export("gzip", RetryPolicy.getDefault());

        assertThat(resultCode.isSuccess()).isTrue();

        ReceivedRequest receivedRequest = receivedRequests.remove();

        assertThat(receivedRequest.headers()).containsEntry("Content-Encoding", "gzip");

        // gzip magic number: the signature was computed over the compressed bytes
        assertThat(receivedRequest.body()[0]).isEqualTo((byte) 0x1f);
        assertThat(receivedRequest.body()[1]).isEqualTo((byte) 0x8b);

        assertSignatureValid(receivedRequest);
    }

    @Test
    void testExportRetriesRetryableStatusAndSignsEveryAttempt() {
        responseStatusCodes.add(503);

        CompletableResultCode resultCode = export(
            "none", RetryPolicy.builder()
                .setInitialBackoff(Duration.ofMillis(10))
                .build());

        assertThat(resultCode.isSuccess()).isTrue();
        assertThat(receivedRequests).hasSize(2);

        receivedRequests.forEach(this::assertSignatureValid);
    }

    @Test
    void testExportFailsOnForbidden() {
        responseStatusCodes.add(403);

        CompletableResultCode resultCode = export("none", RetryPolicy.getDefault());

        assertThat(resultCode.isSuccess()).isFalse();
        assertThat(receivedRequests).hasSize(1);
    }

    @Test
    void testExportFailsWhenCredentialsCannotBeResolved() {
        AtomicInteger credentialsResolutions = new AtomicInteger();

        OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = OtlpHttpLogRecordExporter.builder()
            .setEndpoint(getEndpoint())
            .setComponentLoader(AwsHttpSender.componentLoader(
                new AwsSigV4Signer("us-east-1", "logs", CLOCK), () -> {
                    credentialsResolutions.incrementAndGet();

                    throw new IllegalStateException("Unable to load credentials");
                }))
            .build();

        try {
            CompletableResultCode resultCode = otlpHttpLogRecordExporter.export(List.of(createLogRecordData()))
                .join(10, TimeUnit.SECONDS);

            assertThat(resultCode.isSuccess()).isFalse();
            assertThat(credentialsResolutions).hasValue(1);
            assertThat(receivedRequests).isEmpty();
        } finally {
            otlpHttpLogRecordExporter.shutdown();
        }
    }

    private void assertSignatureValid(ReceivedRequest receivedRequest) {
        Map<String, String> signedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String signedHeaderName : List.of("Content-Type", "Content-Encoding", "x-aws-log-group")) {
            String value = receivedRequest.headers()
                .get(signedHeaderName);

            if (value != null) {
                signedHeaders.put(signedHeaderName, value);
            }
        }

        SdkHttpRequest sdkSignedRequest = AwsSigV4SignerTest.signWithAwsSdk(
            URI.create(getEndpoint()), signedHeaders, receivedRequest.body(),
            AwsSessionCredentialsIdentity.create(
                CREDENTIALS.accessKeyId(), CREDENTIALS.secretAccessKey(), CREDENTIALS.sessionToken()),
            "us-east-1", "logs");

        assertThat(receivedRequest.headers()
            .get("Host")).isEqualTo("localhost:"
                + httpServer.getAddress()
                    .getPort());
        assertThat(receivedRequest.headers()
            .get("Authorization")).isEqualTo(sdkSignedRequest.firstMatchingHeader("Authorization")
                .orElseThrow());
    }

    private CompletableResultCode export(String compression, RetryPolicy retryPolicy) {
        OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = OtlpHttpLogRecordExporter.builder()
            .setEndpoint(getEndpoint())
            .setCompression(compression)
            .setRetryPolicy(retryPolicy)
            .addHeader("x-aws-log-group", "bytechef")
            .setComponentLoader(
                AwsHttpSender.componentLoader(new AwsSigV4Signer("us-east-1", "logs", CLOCK), () -> CREDENTIALS))
            .build();

        try {
            return otlpHttpLogRecordExporter.export(List.of(createLogRecordData()))
                .join(10, TimeUnit.SECONDS);
        } finally {
            otlpHttpLogRecordExporter.shutdown();
        }
    }

    private static LogRecordData createLogRecordData() {
        return TestLogRecordData.builder()
            .setResource(Resource.getDefault())
            .setInstrumentationScopeInfo(InstrumentationScopeInfo.create("bytechef"))
            .setBody("AWS SigV4 signed log record")
            .setSeverity(Severity.INFO)
            .setTimestamp(Instant.now())
            .build();
    }

    private String getEndpoint() {
        return "http://localhost:" + httpServer.getAddress()
            .getPort() + "/v1/logs";
    }

    private record ReceivedRequest(Map<String, String> headers, byte[] body) {
    }
}
