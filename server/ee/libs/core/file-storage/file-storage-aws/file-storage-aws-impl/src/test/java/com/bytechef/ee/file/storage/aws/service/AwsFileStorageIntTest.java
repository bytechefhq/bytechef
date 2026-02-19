/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.jackson.config.JacksonConfiguration;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * @version ee
 *
 * @author Marko Krikovic
 */
@SpringBootTest
@Testcontainers
@Import(JacksonConfiguration.class)
class AwsFileStorageIntTest {

    private static final String BUCKET_NAME = String.valueOf(UUID.randomUUID());
    private static final String DATA = "Hello World";
    private static final String DIR_PATH = "RandomDirectory/Test";
    private static final String TENANT_ID = "public";
    private static final String KEY = "key";
    private static final String FILE_PATH = "s3://" + BUCKET_NAME + "/" + TENANT_ID + "/" + DIR_PATH + "/" + KEY;

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    @Autowired
    private AwsFileStorageServiceImpl storageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.s3.endpoint", () -> String.valueOf(localStack.getEndpointOverride(S3)));
        registry.add("bytechef.file-storage.aws.bucket", () -> BUCKET_NAME);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
    }

    @Test
    void canStoreFileContent() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(msg.getName()).isEqualTo(KEY);
                assertThat(msg.getUrl()).isEqualTo(FILE_PATH);
            });
    }

    @Test
    void canDeterminateIfFileExists() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        boolean existsFileEntry = storageService.fileExists(DIR_PATH, msg);
        boolean existsString = storageService.fileExists(DIR_PATH, KEY);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(existsFileEntry).isTrue();
                assertThat(existsString).isTrue();
            });
    }

    @Test
    void canGetFileEntryUrl() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        URL url = storageService.getFileEntryURL(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(url.toString())
                    .matches(
                        "http://127\\.0\\.0\\.1:\\d+/" + BUCKET_NAME + "/" + TENANT_ID + "/" + DIR_PATH + "/" + KEY);
            });
    }

    @Test
    void canReadFileToBytes() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        byte[] bytes = storageService.readFileToBytes(DIR_PATH, fileEntry);

        String result = new String(bytes, StandardCharsets.UTF_8);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canReadFileToString() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        String result = storageService.readFileToString(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canGetFileStream() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        InputStream fileStream = storageService.getInputStream(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                String result = new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);

                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canGetFileEntry() {
        storageService.storeFileContent(DIR_PATH, KEY, DATA);

        FileEntry result = storageService.getFileEntry(DIR_PATH, KEY);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result.getName()).isEqualTo(KEY);
                assertThat(result.getUrl()).isEqualTo(FILE_PATH);
            });
    }

    @Test
    void canGetFileEntries() {
        FileEntry fileEntry1 = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        FileEntry fileEntry2 = storageService.storeFileContent(DIR_PATH, "key2", DATA);
        Set<FileEntry> fileEntries = storageService.getFileEntries(TENANT_ID + "/" + DIR_PATH);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(fileEntries).contains(fileEntry1);
                assertThat(fileEntries).contains(fileEntry2);
            });
    }

    @Test
    void canDeleteFile() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        storageService.deleteFile(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                boolean exists = storageService.fileExists(DIR_PATH, KEY);

                assertThat(exists).isFalse();
            });
    }

    @Configuration
    @ComponentScan("io.awspring.cloud")
    @EnableConfigurationProperties(ApplicationProperties.class)
    @EnableAutoConfiguration
    static class AwsFileStorageIntTestConfiguration {

        @Bean
        AwsFileStorageServiceImpl awsFileStorageService(
            S3Template s3Template, ApplicationProperties applicationProperties) {

            return new AwsFileStorageServiceImpl(s3Template, applicationProperties.getFileStorage()
                .getAws()
                .getBucket());
        }

        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return () -> AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey());
        }

        @Bean
        AwsRegionProvider awsRegionProvider() {
            return () -> Region.US_EAST_1;
        }
    }
}
