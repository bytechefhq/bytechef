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

package com.bytechef.ee.encryption.aws.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KMS;

import com.bytechef.encryption.EncryptionKey;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AwsKmsEncryptionKeyIntTestConfiguration.class)
@Testcontainers
class AwsKmsEncryptionKeyIntTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(KMS);

    static Path dataKeyPath;
    static String testKeyId;

    @BeforeAll
    static void bootstrap() throws IOException {
        dataKeyPath = Files.createTempDirectory("kms-test")
            .resolve("aws-kms-data-key");

        URI endpoint = localStack.getEndpointOverride(KMS);

        try (KmsClient bootstrapClient = KmsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(localStack.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
            .build()) {

            CreateKeyResponse created = bootstrapClient.createKey(CreateKeyRequest.builder()
                .build());

            testKeyId = created.keyMetadata()
                .keyId();
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("bytechef.cloud.provider", () -> "aws");
        registry.add("bytechef.cloud.aws.access-key-id", localStack::getAccessKey);
        registry.add("bytechef.cloud.aws.secret-access-key", localStack::getSecretKey);
        registry.add("bytechef.cloud.aws.region", localStack::getRegion);
        registry.add("bytechef.encryption.provider", () -> "aws-kms");
        registry.add("bytechef.encryption.aws-kms.key-id", () -> testKeyId);
        registry.add("bytechef.encryption.aws-kms.data-key-path", () -> dataKeyPath.toString());
        registry.add(
            "bytechef.encryption.aws-kms.endpoint",
            () -> localStack.getEndpointOverride(KMS)
                .toString());
    }

    @Autowired
    private EncryptionKey encryptionKey;

    @Test
    void testEncryptionKeyResolvesViaLocalStackKms() {
        String key = encryptionKey.getKey();

        assertThat(key).isNotBlank();
        assertThat(dataKeyPath).exists();
    }
}
