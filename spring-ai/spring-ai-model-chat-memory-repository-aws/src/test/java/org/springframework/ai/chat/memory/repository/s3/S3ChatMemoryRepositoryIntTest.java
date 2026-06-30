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

package org.springframework.ai.chat.memory.repository.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

class S3ChatMemoryRepositoryIntTest {

    private static final String BUCKET = "chat-memory-test";

    private static LocalStackContainer localStack;
    private static S3Client s3Client;
    private static S3ChatMemoryRepository repository;

    @BeforeAll
    static void beforeAll() {
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.5"))
            .withServices(Service.S3);

        localStack.start();

        s3Client = S3Client.builder()
            .endpointOverride(URI.create(localStack.getEndpoint()
                .toString()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
            .region(Region.of(localStack.getRegion()))
            .forcePathStyle(true)
            .build();

        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(BUCKET)
            .build());

        repository = S3ChatMemoryRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .build();
    }

    @AfterAll
    static void afterAll() {
        if (localStack != null) {
            localStack.stop();
        }
    }

    @Test
    void testSaveAllAndFindByConversationId() {
        List<Message> messages = List.of(new UserMessage("hello"), new AssistantMessage("hi there"));

        repository.saveAll("conv-1", messages);

        List<Message> loaded = repository.findByConversationId("conv-1");

        assertEquals(2, loaded.size());
        assertEquals("hello", loaded.get(0)
            .getText());
        assertEquals("hi there", loaded.get(1)
            .getText());
    }

    @Test
    void testFindByConversationIdReturnsEmptyForUnknown() {
        assertTrue(repository.findByConversationId("does-not-exist")
            .isEmpty());
    }

    @Test
    void testFindConversationIdsOrderedByLastModified() throws Exception {
        repository.saveAll("older", List.of(new UserMessage("a")));

        Thread.sleep(1100);

        repository.saveAll("newer", List.of(new UserMessage("b")));

        List<String> ids = repository.findConversationIds();

        assertTrue(ids.indexOf("newer") < ids.indexOf("older"), "most recently modified conversation must come first");
    }

    @Test
    void testDeleteByConversationId() {
        repository.saveAll("to-delete", List.of(new UserMessage("x")));

        repository.deleteByConversationId("to-delete");

        assertTrue(repository.findByConversationId("to-delete")
            .isEmpty());
    }

    @Test
    void testKeyPrefixRoundTrip() {
        S3ChatMemoryRepository prefixed = S3ChatMemoryRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .keyPrefix("tenant-42/")
            .build();

        prefixed.saveAll("conv-pfx", List.of(new UserMessage("scoped")));

        assertEquals(1, prefixed.findByConversationId("conv-pfx")
            .size());
        assertTrue(prefixed.findConversationIds()
            .contains("conv-pfx"));
    }
}
