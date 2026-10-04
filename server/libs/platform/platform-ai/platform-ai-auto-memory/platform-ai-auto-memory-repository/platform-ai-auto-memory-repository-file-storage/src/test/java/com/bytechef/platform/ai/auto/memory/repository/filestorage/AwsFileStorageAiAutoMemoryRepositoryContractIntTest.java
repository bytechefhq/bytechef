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

package com.bytechef.platform.ai.auto.memory.repository.filestorage;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.bytechef.ee.file.storage.aws.service.AwsFileStorageServiceImpl;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepositoryContractTests;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest
@Testcontainers
class AwsFileStorageAiAutoMemoryRepositoryContractIntTest extends AiAutoMemoryRepositoryContractTests {

    private static final String BUCKET_NAME = String.valueOf(UUID.randomUUID());

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Template s3Template;

    private FileStorageAiAutoMemoryRepository fileStorageAiAutoMemoryRepository;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.s3.endpoint", () -> String.valueOf(localStack.getEndpointOverride(S3)));
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
    }

    @BeforeEach
    void beforeEach() {
        fileStorageAiAutoMemoryRepository = new FileStorageAiAutoMemoryRepository(
            new AwsFileStorageServiceImpl(s3Client, s3Template, BUCKET_NAME));
    }

    @AfterEach
    void afterEach() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
            .bucket(BUCKET_NAME)
            .build();

        for (S3Object s3Object : s3Client.listObjectsV2Paginator(listObjectsV2Request)
            .contents()) {

            s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME)
                .key(s3Object.key()));
        }
    }

    @Override
    protected AiAutoMemoryRepository getAiAutoMemoryRepository() {
        return fileStorageAiAutoMemoryRepository;
    }

    @Configuration
    @ImportAutoConfiguration({
        io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class
    })
    static class AwsFileStorageAiAutoMemoryRepositoryContractIntTestConfiguration {

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
