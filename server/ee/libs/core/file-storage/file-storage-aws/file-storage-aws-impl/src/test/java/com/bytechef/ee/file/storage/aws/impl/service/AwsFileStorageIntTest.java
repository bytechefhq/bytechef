/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.ee.file.storage.aws.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbAutoConfiguration;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStreamProvider;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@EnableAutoConfiguration(exclude = DynamoDbAutoConfiguration.class)
@Testcontainers
class AwsFileStorageIntTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    static final String BUCKET_NAME = UUID.randomUUID()
        .toString();
    static final String DATA = "Hello World";
    static final String DIR_PATH = "RandomDirectory/Test";
    static final String KEY = "key";
    static final String FILE_PATH = "s3://" + BUCKET_NAME + "/" + DIR_PATH + "/" + KEY;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.cloud.aws.region.static",
            () -> localStack.getRegion());
        registry.add(
            "spring.cloud.aws.credentials.access-key",
            () -> localStack.getAccessKey());
        registry.add(
            "spring.cloud.aws.credentials.secret-key",
            () -> localStack.getSecretKey());
        registry.add(
            "spring.cloud.aws.s3.endpoint",
            () -> localStack.getEndpointOverride(S3)
                .toString());
        registry.add(
            "bytechef.file-storage.aws.bucket",
            () -> BUCKET_NAME);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
    }

    @Autowired
    AwsFileStorageServiceImpl storageService;

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
    void canDeterminteIfFileExists() {
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
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        URL url = storageService.getFileEntryURL(DIR_PATH, msg);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(url.toString())
                    .matches("http://127\\.0\\.0\\.1:\\d+/" + BUCKET_NAME + "/" + DIR_PATH + "/" + KEY);
            });
    }

    @Test
    void canReadFileToBytes() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        byte[] bytes = storageService.readFileToBytes(DIR_PATH, msg);
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
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        String result = storageService.readFileToString(DIR_PATH, msg);

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
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        InputStream fileStream = storageService.getFileStream(DIR_PATH, msg);

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
        FileEntry msg1 = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        FileEntry msg2 = storageService.storeFileContent(DIR_PATH, "key2", DATA);
        Set<FileEntry> fileEntries = storageService.getFileEntries(DIR_PATH);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(fileEntries).contains(msg1);
                assertThat(fileEntries).contains(msg2);
            });
    }

    @Test
    void canDeleteFile() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        storageService.deleteFile(DIR_PATH, msg);

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
        AwsFileStorageServiceImpl
            awsFileStorageService(S3Template s3Template, ApplicationProperties applicationProperties) {
            return new AwsFileStorageServiceImpl(s3Template, applicationProperties.getFileStorage()
                .getAws()
                .getBucket());
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        S3Template s3Template(
            S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
            S3ObjectConverter s3ObjectConverter, S3Presigner s3Presigner) {
            return new S3Template(s3Client, s3OutputStreamProvider, s3ObjectConverter, s3Presigner);
        }

        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return new AwsCredentialsProvider() {

                @Override
                public AwsCredentials resolveCredentials() {
                    return AwsBasicCredentials.create("noop", "noop");
                }
            };
        }

        @Bean
        AwsRegionProvider awsRegionProvider() {
            return new AwsRegionProvider() {
                @Override
                public Region getRegion() {
                    return Region.US_EAST_1;
                }
            };
        }
    }
}
