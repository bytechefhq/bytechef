/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.aws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * @version ee
 *
 * @author Igor Beslic
 */
class AwsSigV4SignerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void testSignMatchesAwsSdkSignerForOtlpLogsRequest() {
        URI uri = URI.create("https://logs.eu-central-1.amazonaws.com/v1/logs");
        byte[] payload = "otlp protobuf payload é".getBytes(StandardCharsets.UTF_8);

        Map<String, String> headers = new LinkedHashMap<>();

        headers.put("Content-Type", "application/x-protobuf");
        headers.put("x-aws-log-group", "bytechef");
        headers.put("x-aws-log-stream", "  server   app ");

        AwsSigV4Signer.Credentials credentials = new AwsSigV4Signer.Credentials(
            "ASIAEXAMPLE", "secretExample", "sessionTokenExample");

        Map<String, String> signatureHeaders = new AwsSigV4Signer("eu-central-1", "logs", CLOCK).sign(
            "POST", uri, headers, payload, credentials);

        SdkHttpRequest sdkSignedRequest = signWithAwsSdk(
            uri, headers, payload,
            AwsSessionCredentialsIdentity.create(
                credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken()),
            "eu-central-1", "logs");

        assertThat(signatureHeaders).containsEntry("X-Amz-Security-Token", "sessionTokenExample")
            .containsEntry("X-Amz-Content-Sha256", sdkSignedRequest.firstMatchingHeader("X-Amz-Content-Sha256")
                .orElseThrow())
            .containsEntry("X-Amz-Date", sdkSignedRequest.firstMatchingHeader("X-Amz-Date")
                .orElseThrow())
            .containsEntry("Authorization", sdkSignedRequest.firstMatchingHeader("Authorization")
                .orElseThrow());
    }

    @Test
    void testSignMatchesAwsSdkSignerForNonDefaultPortPathAndQuery() {
        URI uri = URI.create("http://localhost:4318/v1/some%20path/logs?b=2&a=x%2Fy&a=1");
        byte[] payload = {
            1, 2, 3
        };

        Map<String, String> headers = Map.of("Content-Type", "application/x-protobuf");

        AwsSigV4Signer.Credentials credentials = new AwsSigV4Signer.Credentials(
            "AKIDEXAMPLE", "secretExample", null);

        Map<String, String> signatureHeaders = new AwsSigV4Signer("us-east-1", "xray", CLOCK).sign(
            "POST", uri, headers, payload, credentials);

        SdkHttpRequest sdkSignedRequest = signWithAwsSdk(
            uri, headers, payload, AwsCredentialsIdentity.create("AKIDEXAMPLE", "secretExample"), "us-east-1",
            "xray");

        assertThat(signatureHeaders.get("Authorization")).isEqualTo(
            sdkSignedRequest.firstMatchingHeader("Authorization")
                .orElseThrow());
    }

    @Test
    void testSignDoesNotSignHeadersRewrittenByProxies() {
        Map<String, String> signatureHeaders = new AwsSigV4Signer("us-east-1", "logs", CLOCK).sign(
            "POST", URI.create("https://logs.us-east-1.amazonaws.com/v1/logs"),
            Map.of("Content-Type", "application/x-protobuf", "User-Agent", "OTel-OTLP-Exporter-Java/1.62.0"),
            new byte[0], new AwsSigV4Signer.Credentials("AKIDEXAMPLE", "secretExample", null));

        assertThat(signatureHeaders.get("Authorization"))
            .contains("SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date,");
    }

    @Test
    void testGetHostHeaderOmitsDefaultPort() {
        assertThat(AwsSigV4Signer.getHostHeader(URI.create("https://logs.us-east-1.amazonaws.com/v1/logs")))
            .isEqualTo("logs.us-east-1.amazonaws.com");
        assertThat(AwsSigV4Signer.getHostHeader(URI.create("https://logs.us-east-1.amazonaws.com:443/v1/logs")))
            .isEqualTo("logs.us-east-1.amazonaws.com");
        assertThat(AwsSigV4Signer.getHostHeader(URI.create("http://localhost:4318/v1/logs")))
            .isEqualTo("localhost:4318");
    }

    @Test
    void testCredentialsToStringMasksSecrets() {
        AwsSigV4Signer.Credentials credentials = new AwsSigV4Signer.Credentials(
            "AKIDEXAMPLE", "secretExample", "sessionTokenExample");

        assertThat(credentials.toString()).contains("AKIDEXAMPLE")
            .doesNotContain("secretExample", "sessionTokenExample");
    }

    static SdkHttpRequest signWithAwsSdk(
        URI uri, Map<String, String> headers, byte[] payload, AwsCredentialsIdentity credentialsIdentity,
        String region, String service) {

        SdkHttpRequest.Builder requestBuilder = SdkHttpRequest.builder()
            .method(SdkHttpMethod.POST)
            .uri(uri);

        headers.forEach(requestBuilder::putHeader);

        SignedRequest signedRequest = AwsV4HttpSigner.create()
            .sign(signRequest -> signRequest.identity(credentialsIdentity)
                .request(requestBuilder.build())
                .payload(ContentStreamProvider.fromByteArray(payload))
                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, service)
                .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                .putProperty(AwsV4HttpSigner.SIGNING_CLOCK, CLOCK));

        return signedRequest.request();
    }
}
