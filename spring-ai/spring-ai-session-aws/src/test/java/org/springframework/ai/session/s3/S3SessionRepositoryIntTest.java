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

package org.springframework.ai.session.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

class S3SessionRepositoryIntTest {

    private static final String BUCKET = "session-test";

    private static LocalStackContainer localStack;
    private static S3Client s3Client;
    private static S3SessionRepository repository;

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

        repository = S3SessionRepository.builder()
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

    private Session newSession(String id) {
        return repository.save(Session.builder()
            .id(id)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());
    }

    private SessionEvent event(String sessionId, String text) {
        return SessionEvent.builder()
            .sessionId(sessionId)
            .message(new UserMessage(text))
            .build();
    }

    @Test
    void testSaveAndFindById() {
        newSession("s-find");

        assertTrue(repository.findById("s-find")
            .isPresent());
        assertEquals("user-1", repository.findById("s-find")
            .get()
            .userId());
    }

    @Test
    void testAppendEventIncrementsVersionAndIsReadable() {
        newSession("s-append");

        assertEquals(0L, repository.getEventVersion("s-append"));

        repository.appendEvent(event("s-append", "hello"));

        assertEquals(1L, repository.getEventVersion("s-append"));

        List<SessionEvent> events = repository.findEvents("s-append", EventFilter.all());

        assertEquals(1, events.size());
        assertEquals("hello", events.get(0)
            .getMessage()
            .getText());
    }

    @Test
    void testAppendEventOnMissingSessionThrows() {
        assertThrows(IllegalArgumentException.class, () -> repository.appendEvent(event("nope", "x")));
    }

    @Test
    void testFindEventsReturnsEmptyForUnknownSession() {
        assertTrue(repository.findEvents("unknown", EventFilter.all())
            .isEmpty());
    }

    @Test
    void testFindEventsLastN() {
        newSession("s-lastn");

        repository.appendEvent(event("s-lastn", "one"));
        repository.appendEvent(event("s-lastn", "two"));
        repository.appendEvent(event("s-lastn", "three"));

        List<SessionEvent> events = repository.findEvents("s-lastn", EventFilter.lastN(2));

        assertEquals(2, events.size());
        assertEquals("two", events.get(0)
            .getMessage()
            .getText());
        assertEquals("three", events.get(1)
            .getMessage()
            .getText());
    }

    @Test
    void testReplaceEventsCasSucceedsThenFailsOnStaleVersion() {
        newSession("s-cas");

        repository.appendEvent(event("s-cas", "v1"));

        long version = repository.getEventVersion("s-cas");

        assertTrue(repository.replaceEvents("s-cas", List.of(event("s-cas", "compacted")), version));
        assertFalse(repository.replaceEvents("s-cas", List.of(event("s-cas", "again")), version));
    }

    @Test
    void testFindByUserIdAndDelete() {
        newSession("s-del");

        assertFalse(repository.findByUserId("user-1")
            .isEmpty());

        repository.delete("s-del");

        assertTrue(repository.findById("s-del")
            .isEmpty());
    }

    @Test
    void testFindExpiredSessionIds() {
        repository.save(Session.builder()
            .id("s-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        assertTrue(repository.findExpiredSessionIds(Instant.now())
            .contains("s-expired"));
    }
}
