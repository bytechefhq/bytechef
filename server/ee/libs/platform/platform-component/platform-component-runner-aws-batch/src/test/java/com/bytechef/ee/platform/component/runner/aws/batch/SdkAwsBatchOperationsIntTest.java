/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bytechef.platform.component.ComponentConnection;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Proves the S3 staging half of the AWS Batch runner against LocalStack. Batch itself is verified by hand, because
 * LocalStack's Batch emulation is not part of its free edition.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class SdkAwsBatchOperationsIntTest {

    private static final String BUCKET = "bytechef-staging";

    private static LocalStackContainer localStackContainer;
    private static URI endpoint;

    @BeforeAll
    static void startLocalStack() {
        assumeTrue(
            DockerClientFactory.instance()
                .isDockerAvailable(),
            "no docker daemon, so LocalStack cannot start");

        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices("s3");

        localStackContainer.start();

        endpoint = localStackContainer.getEndpoint();

        try (S3Client s3Client = S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()) {

            s3Client.createBucket(builder -> builder.bucket(BUCKET));
        }
    }

    @AfterAll
    static void stopLocalStack() {
        if (localStackContainer != null) {
            localStackContainer.stop();
        }
    }

    @Test
    void testAnUploadedInputIsReadableThroughItsPresignedGet() throws Exception {
        Path file = Files.createTempFile("bytechef-int-", ".tar");

        Files.writeString(file, "input-bytes", StandardCharsets.UTF_8);

        try (SdkAwsBatchOperations operations = operations()) {
            operations.putObject(BUCKET, "bytechef/1/k-1/input.tar", file);

            URL url = operations.presignGet(BUCKET, "bytechef/1/k-1/input.tar", Duration.ofMinutes(10));

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(url.toURI())
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("input-bytes");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void testAnOutputPutThroughItsPresignedUrlIsCollected() throws Exception {
        try (SdkAwsBatchOperations operations = operations()) {
            URL url = operations.presignPut(BUCKET, "bytechef/1/k-2/output.tar", Duration.ofMinutes(10));

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(url.toURI())
                    .PUT(HttpRequest.BodyPublishers.ofString("output-bytes"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);

            try (InputStream inputStream = operations.getObject(BUCKET, "bytechef/1/k-2/output.tar")) {
                assertThat(inputStream).isNotNull();
                assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("output-bytes");
            }
        }
    }

    @Test
    void testAMissingOutputReadsAsNull() {
        try (SdkAwsBatchOperations operations = operations()) {
            assertThat(operations.getObject(BUCKET, "bytechef/1/missing/output.tar")).isNull();
        }
    }

    @Test
    void testDeletingAPrefixRemovesEveryObjectUnderIt() throws Exception {
        Path file = Files.createTempFile("bytechef-int-", ".tar");

        Files.writeString(file, "x", StandardCharsets.UTF_8);

        try (SdkAwsBatchOperations operations = operations()) {
            operations.putObject(BUCKET, "bytechef/1/k-3/input.tar", file);
            operations.putObject(BUCKET, "bytechef/1/k-3/output.tar", file);
            operations.putObject(BUCKET, "bytechef/1/k-4/input.tar", file);

            operations.deletePrefix(BUCKET, "bytechef/1/k-3/");

            assertThat(operations.getObject(BUCKET, "bytechef/1/k-3/input.tar")).isNull();
            assertThat(operations.getObject(BUCKET, "bytechef/1/k-3/output.tar")).isNull();

            try (InputStream inputStream = operations.getObject(BUCKET, "bytechef/1/k-4/input.tar")) {
                assertThat(inputStream).isNotNull();
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static SdkAwsBatchOperations operations() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("accessKeyId", "test");
        parameters.put("secretAccessKey", "test");
        parameters.put("region", "us-east-1");

        AwsConnectionSettings connectionSettings = AwsConnectionSettings.of(
            new ComponentConnection("aws", 1, 1L, parameters, null));

        return new SdkAwsBatchOperations(connectionSettings, Duration.ofMinutes(15), endpoint);
    }
}
