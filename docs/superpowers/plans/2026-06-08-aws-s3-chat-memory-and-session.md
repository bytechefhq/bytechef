# AWS S3 Chat Memory & Session Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add AWS S3-backed chat memory and session storage to ByteChef as six implementations — two vendored Spring AI modules (S3 `ChatMemoryRepository`, S3 `SessionRepository`), two connection-backed components, and two built-in (bucket-per-tenant) extensions.

**Architecture:** Two self-contained library modules under a new repo-root `spring-ai/` dir store one JSON object per conversation/session in S3 (the session repo uses S3 conditional writes — `If-Match` on ETag — to realize the `replaceEvents(expectedVersion)` compare-and-swap contract). ByteChef consumes them in two ways: connection-backed components (user supplies the bucket) and built-in platform storage selected by `bytechef.ai.{memory,session}.provider=aws`, where a tenant-routing wrapper resolves a per-tenant bucket `{bucketPrefix}-{tenantId}` from `TenantContext`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0.0-RC1 (`spring-ai-model`), Spring AI Community Session 0.4.2 (`spring-ai-session-management`), AWS SDK v2 (`software.amazon.awssdk:s3`), Jackson 3 (`tools.jackson`), Caffeine, JUnit 5, Testcontainers + LocalStack, Gradle.

**Spec:** `docs/superpowers/specs/2026-06-08-aws-s3-chat-memory-and-session-design.md`

---

## File Structure

### Module 1 — `spring-ai/spring-ai-model-chat-memory-repository-aws`
- `build.gradle.kts`
- `src/main/java/org/springframework/ai/chat/memory/repository/s3/S3ChatMemoryRepository.java` — the repo + nested `Builder`
- `src/main/java/org/springframework/ai/chat/memory/repository/s3/StoredMessage.java` — JSON DTO + Message↔DTO mapping
- `src/test/java/org/springframework/ai/chat/memory/repository/s3/S3ChatMemoryRepositoryIntTest.java`

### Module 2 — `spring-ai/spring-ai-session-aws`
- `build.gradle.kts`
- `src/main/java/org/springframework/ai/session/s3/S3SessionRepository.java` — the repo + nested `Builder`
- `src/main/java/org/springframework/ai/session/s3/StoredSession.java` — `{StoredSession, StoredEvent}` JSON DTOs + mapping
- `src/test/java/org/springframework/ai/session/s3/S3SessionRepositoryIntTest.java`

### Module 3 — component `chat-memory-aws`
- `build.gradle.kts`
- `.../aws/AwsChatMemoryComponentHandler.java`
- `.../aws/cluster/AwsChatMemory.java`
- `.../aws/connection/AwsChatMemoryConnection.java`
- `.../aws/constant/AwsChatMemoryConstants.java`
- `.../aws/util/AwsChatMemoryUtils.java`
- `src/test/.../aws/AwsChatMemoryComponentHandlerTest.java`

### Module 4 — component `chat-memory-aws-session`
- `build.gradle.kts`
- `.../aws/session/AwsSessionChatMemoryComponentHandler.java`
- `.../aws/session/cluster/AwsSessionChatMemory.java`
- `.../aws/session/connection/AwsSessionChatMemoryConnection.java`
- `.../aws/session/constant/AwsSessionChatMemoryConstants.java`
- `.../aws/session/util/AwsSessionChatMemoryUtils.java`
- `src/test/.../aws/session/AwsSessionChatMemoryComponentHandlerTest.java`

### Module 5 — built-in chat memory config `ai-chat-memory-aws-config`
- `build.gradle.kts`
- `.../aws/config/AwsChatMemoryProperties.java`
- `.../aws/config/AwsChatMemoryConfiguration.java`
- `.../aws/config/AwsS3ClientFactory.java`
- `.../aws/config/TenantRoutingS3ChatMemoryRepository.java`
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `src/test/.../aws/config/TenantRoutingS3ChatMemoryRepositoryTest.java`

### Module 6 — built-in session (edit existing `chat-memory-builtin-session`)
- Modify `build.gradle.kts`
- Modify `.../builtin/session/cluster/BuiltInSessionChatMemory.java`
- Create `.../builtin/session/util/TenantRoutingS3SessionRepository.java`
- Create `.../builtin/session/util/BuiltInSessionProperties.java`
- Modify `.../builtin/session/BuiltInSessionChatMemoryComponentHandler.java`
- Modify `src/test/.../builtin/session/BuiltInSessionChatMemoryComponentHandlerTest.java`

### Registration
- `settings.gradle.kts` — add the 5 new modules.

---

## Phase 1 — Spring AI module: S3 ChatMemoryRepository

### Task 1: Scaffold the module and register it

**Files:**
- Create: `spring-ai/spring-ai-model-chat-memory-repository-aws/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

```kotlin
version = "1.0"

dependencies {
    implementation("org.springframework.ai:spring-ai-model")
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    compileOnly("org.jspecify:jspecify")

    testImplementation("org.testcontainers:localstack")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("software.amazon.awssdk:s3")
}
```

- [ ] **Step 2: Register the module in `settings.gradle.kts`**

Find the block of `include(...)` lines near the other `spring-ai-agent-utils` / chat-memory entries and add:

```kotlin
include("spring-ai:spring-ai-model-chat-memory-repository-aws")
```

- [ ] **Step 3: Verify Gradle sees the project**

Run: `./gradlew :spring-ai:spring-ai-model-chat-memory-repository-aws:dependencies --configuration compileClasspath -q | head -5`
Expected: prints the dependency tree (project resolves, no "project not found").

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts spring-ai/spring-ai-model-chat-memory-repository-aws/build.gradle.kts
git commit -m "732 Scaffold spring-ai-model-chat-memory-repository-aws module"
```

### Task 2: `StoredMessage` DTO + Message mapping

**Files:**
- Create: `spring-ai/spring-ai-model-chat-memory-repository-aws/src/main/java/org/springframework/ai/chat/memory/repository/s3/StoredMessage.java`

This mirrors the JDBC repository's `type` / `text` / `data` message split so messages round-trip identically across backends. No test of its own — it is exercised by Task 4's integration test.

- [ ] **Step 1: Write `StoredMessage`**

```java
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

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON-serializable representation of a single Spring AI {@link Message}, mirroring the column split used by the JDBC
 * chat-memory repository: {@code type} (the {@link MessageType} name), {@code text} (plain content), and {@code data}
 * (a JSON blob carrying tool-call / tool-response payloads, {@code null} for plain messages).
 *
 * @author Ivica Cardic
 */
record StoredMessage(String type, String text, @Nullable String data) {

    static StoredMessage from(Message message, JsonMapper jsonMapper) {
        String data = null;

        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            data = jsonMapper.writeValueAsString(assistantMessage.getToolCalls());
        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            data = jsonMapper.writeValueAsString(toolResponseMessage.getResponses());
        }

        return new StoredMessage(message.getMessageType()
            .name(), message.getText(), data);
    }

    Message toMessage(JsonMapper jsonMapper) {
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case ASSISTANT -> {
                if (data != null && !data.isBlank()) {
                    List<AssistantMessage.ToolCall> toolCalls = jsonMapper.readValue(
                        data, new TypeReference<List<AssistantMessage.ToolCall>>() {});

                    yield AssistantMessage.builder()
                        .content(text)
                        .toolCalls(toolCalls)
                        .build();
                }

                yield new AssistantMessage(text);
            }
            case TOOL -> {
                List<ToolResponseMessage.ToolResponse> responses = (data != null && !data.isBlank())
                    ? jsonMapper.readValue(data, new TypeReference<List<ToolResponseMessage.ToolResponse>>() {})
                    : List.of();

                yield ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
            }
        };
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add spring-ai/spring-ai-model-chat-memory-repository-aws/src/main/java/org/springframework/ai/chat/memory/repository/s3/StoredMessage.java
git commit -m "732 Add StoredMessage JSON DTO for S3 chat memory"
```

### Task 3: Failing integration test for `S3ChatMemoryRepository`

**Files:**
- Create: `spring-ai/spring-ai-model-chat-memory-repository-aws/src/test/java/org/springframework/ai/chat/memory/repository/s3/S3ChatMemoryRepositoryIntTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
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
}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :spring-ai:spring-ai-model-chat-memory-repository-aws:test --tests '*S3ChatMemoryRepositoryIntTest*'`
Expected: FAIL — compilation error, `S3ChatMemoryRepository` does not exist.

- [ ] **Step 3: Commit the test**

```bash
git add spring-ai/spring-ai-model-chat-memory-repository-aws/src/test
git commit -m "732 Add failing S3ChatMemoryRepository integration test"
```

### Task 4: Implement `S3ChatMemoryRepository`

**Files:**
- Create: `spring-ai/spring-ai-model-chat-memory-repository-aws/src/main/java/org/springframework/ai/chat/memory/repository/s3/S3ChatMemoryRepository.java`

- [ ] **Step 1: Write the implementation**

```java
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * S3-backed {@link ChatMemoryRepository}. Each conversation is stored as a single JSON object at
 * {@code {keyPrefix}{conversationId}.json} holding the serialized message list. {@code saveAll} replaces the object;
 * {@code findConversationIds} lists objects ordered by last-modified (most recent first).
 *
 * <p>
 * {@code saveAll} is a plain {@code PutObject} (last-writer-wins). Concurrent writes to the <em>same</em> conversation
 * can lose updates — acceptable for the single-writer-per-conversation agent loop.
 *
 * @author Ivica Cardic
 */
public final class S3ChatMemoryRepository implements ChatMemoryRepository {

    private static final String SUFFIX = ".json";

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;
    private final Function<String, String> keyResolver;
    private final JsonMapper jsonMapper;

    private S3ChatMemoryRepository(Builder builder) {
        this.s3Client = builder.s3Client;
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
        this.jsonMapper = builder.jsonMapper;
        this.keyResolver = builder.keyResolver != null
            ? builder.keyResolver
            : conversationId -> this.keyPrefix + conversationId + SUFFIX;
    }

    @Override
    public List<String> findConversationIds() {
        List<S3Object> objects = new ArrayList<>(s3Client.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(keyPrefix)
            .build())
            .contents());

        objects.sort(Comparator.comparing(S3Object::lastModified)
            .reversed());

        List<String> ids = new ArrayList<>();

        for (S3Object object : objects) {
            String key = object.key();

            if (key.endsWith(SUFFIX)) {
                ids.add(key.substring(keyPrefix.length(), key.length() - SUFFIX.length()));
            }
        }

        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        ResponseBytes<GetObjectResponse> response;

        try {
            response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyResolver.apply(conversationId))
                .build());
        } catch (NoSuchKeyException noSuchKeyException) {
            return List.of();
        }

        List<StoredMessage> stored = jsonMapper.readValue(
            response.asByteArray(), new TypeReference<List<StoredMessage>>() {});

        List<Message> messages = new ArrayList<>();

        for (StoredMessage storedMessage : stored) {
            messages.add(storedMessage.toMessage(jsonMapper));
        }

        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        List<StoredMessage> stored = new ArrayList<>();

        for (Message message : messages) {
            stored.add(StoredMessage.from(message, jsonMapper));
        }

        byte[] body = jsonMapper.writeValueAsBytes(stored);

        s3Client.putObject(PutObjectRequest.builder()
            .bucket(bucketName)
            .key(keyResolver.apply(conversationId))
            .contentType("application/json")
            .build(), RequestBody.fromBytes(body));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(keyResolver.apply(conversationId))
            .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private S3Client s3Client;
        private String bucketName;
        private String keyPrefix = "";
        private Function<String, String> keyResolver;
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        private Builder() {
        }

        public Builder s3Client(S3Client s3Client) {
            this.s3Client = s3Client;

            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;

            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;

            return this;
        }

        public Builder keyResolver(Function<String, String> keyResolver) {
            this.keyResolver = keyResolver;

            return this;
        }

        public Builder jsonMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;

            return this;
        }

        public S3ChatMemoryRepository build() {
            Assert.notNull(s3Client, "s3Client must not be null");
            Assert.hasText(bucketName, "bucketName must not be null or empty");

            return new S3ChatMemoryRepository(this);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :spring-ai:spring-ai-model-chat-memory-repository-aws:test --tests '*S3ChatMemoryRepositoryIntTest*'`
Expected: PASS (all 4 tests). Requires Docker running for LocalStack.

- [ ] **Step 3: Format and commit**

```bash
./gradlew :spring-ai:spring-ai-model-chat-memory-repository-aws:spotlessApply
git add spring-ai/spring-ai-model-chat-memory-repository-aws/src/main
git commit -m "732 Implement S3ChatMemoryRepository"
```

---

## Phase 2 — Spring AI module: S3 SessionRepository

### Task 5: Scaffold the module and register it

**Files:**
- Create: `spring-ai/spring-ai-session-aws/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

```kotlin
version = "1.0"

dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation("org.springframework.ai:spring-ai-model")
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    compileOnly("org.jspecify:jspecify")

    testImplementation("org.testcontainers:localstack")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

- [ ] **Step 2: Register in `settings.gradle.kts`**

```kotlin
include("spring-ai:spring-ai-session-aws")
```

- [ ] **Step 3: Verify**

Run: `./gradlew :spring-ai:spring-ai-session-aws:dependencies --configuration compileClasspath -q | head -5`
Expected: dependency tree prints, project resolves.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts spring-ai/spring-ai-session-aws/build.gradle.kts
git commit -m "732 Scaffold spring-ai-session-aws module"
```

### Task 6: `StoredSession` / `StoredEvent` DTOs + mapping

**Files:**
- Create: `spring-ai/spring-ai-session-aws/src/main/java/org/springframework/ai/session/s3/StoredSession.java`

- [ ] **Step 1: Write the DTOs and mapping helpers**

```java
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON document persisted as one S3 object per session: session metadata, the full event log, and a monotonically
 * increasing {@code version} used for compare-and-swap on event-log replacement.
 *
 * @author Ivica Cardic
 */
record StoredSession(
    String id, String userId, long createdAtEpochMilli, @Nullable Long expiresAtEpochMilli,
    Map<String, Object> metadata, long version, List<StoredEvent> events) {

    static StoredSession fromSession(Session session, long version, List<StoredEvent> events) {
        Instant expiresAt = session.expiresAt();

        return new StoredSession(
            session.id(), session.userId(), session.createdAt()
                .toEpochMilli(),
            expiresAt != null ? expiresAt.toEpochMilli() : null, new HashMap<>(session.metadata()), version, events);
    }

    Session toSession() {
        Session.Builder builder = Session.builder()
            .id(id)
            .userId(userId)
            .createdAt(Instant.ofEpochMilli(createdAtEpochMilli))
            .metadata(metadata);

        if (expiresAtEpochMilli != null) {
            builder.expiresAt(Instant.ofEpochMilli(expiresAtEpochMilli));
        }

        return builder.build();
    }

    record StoredEvent(
        String id, String sessionId, long timestampEpochMilli, String messageType, String messageContent,
        @Nullable String messageData, boolean synthetic, @Nullable String branch, Map<String, Object> metadata) {

        static StoredEvent fromEvent(SessionEvent event, JsonMapper jsonMapper) {
            Message message = event.getMessage();

            String messageData = null;

            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                messageData = jsonMapper.writeValueAsString(assistantMessage.getToolCalls());
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                messageData = jsonMapper.writeValueAsString(toolResponseMessage.getResponses());
            }

            return new StoredEvent(
                event.getId(), event.getSessionId(), event.getTimestamp()
                    .toEpochMilli(),
                message.getMessageType()
                    .name(),
                message.getText(), messageData, event.isSynthetic(), event.getBranch(),
                new HashMap<>(event.getMetadata()));
        }

        SessionEvent toEvent(JsonMapper jsonMapper) {
            Map<String, Object> mergedMetadata = new HashMap<>(metadata);

            if (synthetic) {
                mergedMetadata.put(SessionEvent.METADATA_SYNTHETIC, true);
            }

            return SessionEvent.builder()
                .id(id)
                .sessionId(sessionId)
                .timestamp(Instant.ofEpochMilli(timestampEpochMilli))
                .message(toMessage(jsonMapper))
                .branch(branch)
                .metadata(mergedMetadata)
                .build();
        }

        private Message toMessage(JsonMapper jsonMapper) {
            return switch (MessageType.valueOf(messageType)) {
                case USER -> new UserMessage(messageContent);
                case SYSTEM -> new SystemMessage(messageContent);
                case ASSISTANT -> {
                    if (messageData != null && !messageData.isBlank()) {
                        List<AssistantMessage.ToolCall> toolCalls = jsonMapper.readValue(
                            messageData, new TypeReference<List<AssistantMessage.ToolCall>>() {});

                        yield AssistantMessage.builder()
                            .content(messageContent)
                            .toolCalls(toolCalls)
                            .build();
                    }

                    yield new AssistantMessage(messageContent);
                }
                case TOOL -> {
                    List<ToolResponseMessage.ToolResponse> responses = (messageData != null && !messageData.isBlank())
                        ? jsonMapper.readValue(
                            messageData, new TypeReference<List<ToolResponseMessage.ToolResponse>>() {})
                        : List.of();

                    yield ToolResponseMessage.builder()
                        .responses(responses)
                        .build();
                }
            };
        }
    }

    static List<StoredEvent> toStoredEvents(List<SessionEvent> events, JsonMapper jsonMapper) {
        List<StoredEvent> stored = new ArrayList<>();

        for (SessionEvent event : events) {
            stored.add(StoredEvent.fromEvent(event, jsonMapper));
        }

        return stored;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add spring-ai/spring-ai-session-aws/src/main/java/org/springframework/ai/session/s3/StoredSession.java
git commit -m "732 Add StoredSession/StoredEvent JSON DTOs for S3 session repository"
```

### Task 7: Failing integration test for `S3SessionRepository`

**Files:**
- Create: `spring-ai/spring-ai-session-aws/src/test/java/org/springframework/ai/session/s3/S3SessionRepositoryIntTest.java`

- [ ] **Step 1: Write the failing test (covers lifecycle, events, filtering, and CAS)**

```java
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

        // second swap with the now-stale version must fail
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
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :spring-ai:spring-ai-session-aws:test --tests '*S3SessionRepositoryIntTest*'`
Expected: FAIL — `S3SessionRepository` does not exist.

- [ ] **Step 3: Commit**

```bash
git add spring-ai/spring-ai-session-aws/src/test
git commit -m "732 Add failing S3SessionRepository integration test"
```

### Task 8: Implement `S3SessionRepository`

**Files:**
- Create: `spring-ai/spring-ai-session-aws/src/main/java/org/springframework/ai/session/s3/S3SessionRepository.java`

The CAS path uses S3 conditional writes: read the object + its ETag, then `PutObject(...).ifMatch(etag)`. A `412` (surfaced as `S3Exception` with `statusCode()==412`) means a concurrent writer won — return `false`.

- [ ] **Step 1: Write the implementation**

```java
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.s3.StoredSession.StoredEvent;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

/**
 * S3-backed {@link SessionRepository}. Each session is one JSON object at {@code {keyPrefix}{sessionId}.json} holding
 * session metadata, the full event log, and a {@code version} counter. Compare-and-swap event replacement is realized
 * with S3 conditional writes ({@code If-Match} on the object ETag); a {@code 412 PreconditionFailed} maps to a
 * {@code false} return. {@code findByUserId} / {@code findExpiredSessionIds} are {@code ListObjects} + scan (O(n)).
 *
 * @author Ivica Cardic
 */
public final class S3SessionRepository implements SessionRepository {

    private static final String SUFFIX = ".json";
    private static final int MAX_APPEND_RETRIES = 5;

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;
    private final Function<String, String> keyResolver;
    private final JsonMapper jsonMapper;

    private S3SessionRepository(Builder builder) {
        this.s3Client = builder.s3Client;
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
        this.jsonMapper = builder.jsonMapper;
        this.keyResolver = builder.keyResolver != null
            ? builder.keyResolver
            : sessionId -> this.keyPrefix + sessionId + SUFFIX;
    }

    @Override
    public Session save(Session session) {
        Assert.notNull(session, "session must not be null");

        Loaded existing = load(session.id());

        long version = existing != null ? existing.document.version() : 0L;
        List<StoredEvent> events = existing != null ? existing.document.events() : List.of();

        put(StoredSession.fromSession(session, version, events), null);

        return session;
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        Loaded loaded = load(sessionId);

        return loaded == null ? Optional.empty() : Optional.of(loaded.document.toSession());
    }

    @Override
    public List<Session> findByUserId(String userId) {
        Assert.hasText(userId, "userId must not be null or empty");

        List<Session> sessions = new ArrayList<>();

        for (StoredSession document : loadAll()) {
            if (userId.equals(document.userId())) {
                sessions.add(document.toSession());
            }
        }

        return sessions;
    }

    @Override
    public List<String> findExpiredSessionIds(Instant before) {
        Assert.notNull(before, "before must not be null");

        List<String> ids = new ArrayList<>();

        for (StoredSession document : loadAll()) {
            Long expiresAt = document.expiresAtEpochMilli();

            if (expiresAt != null && expiresAt < before.toEpochMilli()) {
                ids.add(document.id());
            }
        }

        return ids;
    }

    @Override
    public void delete(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(keyResolver.apply(sessionId))
            .build());
    }

    @Override
    public void appendEvent(SessionEvent event) {
        Assert.notNull(event, "event must not be null");

        String sessionId = event.getSessionId();

        for (int attempt = 0; attempt < MAX_APPEND_RETRIES; attempt++) {
            Loaded loaded = requireSession(sessionId);

            List<StoredEvent> events = new ArrayList<>(loaded.document.events());

            events.add(StoredEvent.fromEvent(event, jsonMapper));

            StoredSession next = withEvents(loaded.document, events, loaded.document.version() + 1);

            if (tryPut(next, loaded.etag)) {
                return;
            }
        }

        throw new IllegalStateException("Failed to append event after " + MAX_APPEND_RETRIES + " attempts: " + sessionId);
    }

    @Override
    public void replaceEvents(String sessionId, List<SessionEvent> events) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(events, "events must not be null");

        Loaded loaded = requireSession(sessionId);

        StoredSession next = withEvents(
            loaded.document, StoredSession.toStoredEvents(events, jsonMapper), loaded.document.version() + 1);

        put(next, null);
    }

    @Override
    public boolean replaceEvents(String sessionId, List<SessionEvent> events, long expectedVersion) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(events, "events must not be null");

        Loaded loaded = requireSession(sessionId);

        if (loaded.document.version() != expectedVersion) {
            return false;
        }

        StoredSession next = withEvents(
            loaded.document, StoredSession.toStoredEvents(events, jsonMapper), loaded.document.version() + 1);

        return tryPut(next, loaded.etag);
    }

    @Override
    public long getEventVersion(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        Loaded loaded = load(sessionId);

        return loaded == null ? 0L : loaded.document.version();
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(filter, "filter must not be null");

        Loaded loaded = load(sessionId);

        if (loaded == null) {
            return List.of();
        }

        List<SessionEvent> events = new ArrayList<>();

        for (StoredEvent storedEvent : loaded.document.events()) {
            events.add(storedEvent.toEvent(jsonMapper));
        }

        return applyFilter(events, filter);
    }

    private List<SessionEvent> applyFilter(List<SessionEvent> events, EventFilter filter) {
        List<SessionEvent> matched = new ArrayList<>();

        for (SessionEvent event : events) {
            if (matches(event, filter)) {
                matched.add(event);
            }
        }

        matched.sort((left, right) -> left.getTimestamp()
            .compareTo(right.getTimestamp()));

        if (filter.lastN() != null) {
            int from = Math.max(0, matched.size() - filter.lastN());

            return Collections.unmodifiableList(new ArrayList<>(matched.subList(from, matched.size())));
        }

        if (filter.pageSize() != null) {
            int page = filter.page() != null ? filter.page() : 0;
            int from = Math.min(page * filter.pageSize(), matched.size());
            int to = Math.min(from + filter.pageSize(), matched.size());

            return Collections.unmodifiableList(new ArrayList<>(matched.subList(from, to)));
        }

        return Collections.unmodifiableList(matched);
    }

    private boolean matches(SessionEvent event, EventFilter filter) {
        Instant timestamp = event.getTimestamp();

        if (filter.from() != null && timestamp.isBefore(filter.from())) {
            return false;
        }

        if (filter.to() != null && timestamp.isAfter(filter.to())) {
            return false;
        }

        if (filter.messageTypes() != null) {
            MessageType type = event.getMessage()
                .getMessageType();

            if (!filter.messageTypes()
                .contains(type)) {
                return false;
            }
        }

        if (filter.excludeSynthetic() && event.isSynthetic()) {
            return false;
        }

        if (filter.branch() != null && !branchVisible(event.getBranch(), filter.branch())) {
            return false;
        }

        if (filter.keyword() != null) {
            String text = event.getMessage()
                .getText();

            if (text == null || !text.toLowerCase()
                .contains(filter.keyword())) {
                return false;
            }
        }

        return true;
    }

    /**
     * An event at {@code eventBranch} is visible to an agent at {@code filterBranch} when the event branch is null (a
     * root event), equals the filter branch, or is a dot-prefix ancestor of it.
     */
    private boolean branchVisible(@Nullable String eventBranch, String filterBranch) {
        return eventBranch == null || eventBranch.equals(filterBranch) || filterBranch.startsWith(eventBranch + ".");
    }

    private StoredSession withEvents(StoredSession document, List<StoredEvent> events, long version) {
        return new StoredSession(
            document.id(), document.userId(), document.createdAtEpochMilli(), document.expiresAtEpochMilli(),
            document.metadata(), version, events);
    }

    private Loaded requireSession(String sessionId) {
        Loaded loaded = load(sessionId);

        if (loaded == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return loaded;
    }

    @Nullable
    private Loaded load(String sessionId) {
        ResponseBytes<GetObjectResponse> response;

        try {
            response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyResolver.apply(sessionId))
                .build());
        } catch (NoSuchKeyException noSuchKeyException) {
            return null;
        }

        StoredSession document = jsonMapper.readValue(response.asByteArray(), StoredSession.class);

        return new Loaded(document, response.response()
            .eTag());
    }

    private List<StoredSession> loadAll() {
        List<StoredSession> documents = new ArrayList<>();

        List<S3Object> objects = s3Client.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(keyPrefix)
            .build())
            .contents();

        for (S3Object object : objects) {
            String key = object.key();

            if (!key.endsWith(SUFFIX)) {
                continue;
            }

            try {
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

                documents.add(jsonMapper.readValue(response.asByteArray(), StoredSession.class));
            } catch (NoSuchKeyException noSuchKeyException) {
                // object deleted between list and get — skip
            }
        }

        return documents;
    }

    private void put(StoredSession document, @Nullable String ifMatchEtag) {
        PutObjectRequest.Builder request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(keyResolver.apply(document.id()))
            .contentType("application/json");

        if (ifMatchEtag != null) {
            request.ifMatch(ifMatchEtag);
        }

        s3Client.putObject(request.build(), RequestBody.fromBytes(jsonMapper.writeValueAsBytes(document)));
    }

    private boolean tryPut(StoredSession document, @Nullable String ifMatchEtag) {
        try {
            put(document, ifMatchEtag);

            return true;
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() == 412) {
                return false;
            }

            throw s3Exception;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private record Loaded(StoredSession document, @Nullable String etag) {
    }

    public static final class Builder {

        private S3Client s3Client;
        private String bucketName;
        private String keyPrefix = "";
        private Function<String, String> keyResolver;
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        private Builder() {
        }

        public Builder s3Client(S3Client s3Client) {
            this.s3Client = s3Client;

            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;

            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;

            return this;
        }

        public Builder keyResolver(Function<String, String> keyResolver) {
            this.keyResolver = keyResolver;

            return this;
        }

        public Builder jsonMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;

            return this;
        }

        public S3SessionRepository build() {
            Assert.notNull(s3Client, "s3Client must not be null");
            Assert.hasText(bucketName, "bucketName must not be null or empty");

            return new S3SessionRepository(this);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :spring-ai:spring-ai-session-aws:test --tests '*S3SessionRepositoryIntTest*'`
Expected: PASS (all 8 tests). Docker required.

- [ ] **Step 3: Format and commit**

```bash
./gradlew :spring-ai:spring-ai-session-aws:spotlessApply
git add spring-ai/spring-ai-session-aws/src/main
git commit -m "732 Implement S3SessionRepository"
```

---

## Phase 3 — Component: `chat-memory-aws`

### Task 9: Scaffold module + constants + connection

**Files:**
- Create: `server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/build.gradle.kts`
- Create: `.../chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/constant/AwsChatMemoryConstants.java`
- Create: `.../chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/connection/AwsChatMemoryConnection.java`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

```kotlin
version = "1.0"

dependencies {
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-model-chat-memory-repository-aws"))
}
```

- [ ] **Step 2: Register in `settings.gradle.kts`** (alongside the other `chat-memory-*` includes)

```kotlin
include("server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws")
```

- [ ] **Step 3: Write the constants**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.constant;

/**
 * @author Ivica Cardic
 */
public final class AwsChatMemoryConstants {

    public static final String AWS_CHAT_MEMORY = "awsChatMemory";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String REGION = "region";
    public static final String BUCKET = "bucket";
    public static final String KEY_PREFIX = "keyPrefix";
    public static final String CONVERSATION_ID = "conversationId";

    private AwsChatMemoryConstants() {
    }
}
```

- [ ] **Step 4: Write the connection** (region option list condensed; mirror the full list from `AwsS3Connection` if exhaustiveness is desired)

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.connection;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public final class AwsChatMemoryConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .required(true)
                        .defaultValue("us-east-1"),
                    string(BUCKET)
                        .label("Bucket")
                        .required(true),
                    string(KEY_PREFIX)
                        .label("Key Prefix")
                        .description("Optional prefix prepended to every conversation object key.")
                        .required(false)));

    private AwsChatMemoryConnection() {
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws
git commit -m "732 Scaffold chat-memory-aws component (constants + connection)"
```

### Task 10: `AwsChatMemoryUtils` — S3 client + repository + options

**Files:**
- Create: `.../chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/util/AwsChatMemoryUtils.java`

- [ ] **Step 1: Write the util**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.MultipleConnectionsOptionsFunction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.repository.s3.S3ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public final class AwsChatMemoryUtils {

    private AwsChatMemoryUtils() {
    }

    public static S3ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        S3Client s3Client = S3Client.builder()
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY))))
            .build();

        return S3ChatMemoryRepository.builder()
            .s3Client(s3Client)
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
            .build();
    }

    public static MultipleConnectionsOptionsFunction<String> getFirstMessages() {
        return (inputParameters, componentConnections, extensions, context) -> {
            // chat-memory-aws carries a single connection; options are looked up from it
            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            return options;
        };
    }
}
```

Note: the `getFirstMessages` options function mirrors the JDBC component but, because the AWS component
carries its connection directly on the cluster element, the dropdown is populated by the cluster element's
own options function in Task 11 (`AwsChatMemory`). This util method is a stub kept for parity and returns
an empty list; the working options logic lives in the cluster element. (If a connection-aware options API
is required, follow `JdbcChatMemoryUtils.getClusterElementFirstMessages` and resolve via
`ClusterElementContextAware`.)

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/util
git commit -m "732 Add AwsChatMemoryUtils (S3 client + repository factory)"
```

### Task 11: `AwsChatMemory` cluster element

**Files:**
- Create: `.../chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/cluster/AwsChatMemory.java`

- [ ] **Step 1: Write the cluster element**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.cluster;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.ai.agent.chat.memory.aws.util.AwsChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * @author Ivica Cardic
 */
public final class AwsChatMemory {

    public static ClusterElementDefinition<ChatMemoryFunction> of() {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("AWS S3 Chat Memory")
            .description("Memory is retrieved from an Amazon S3 bucket and added as prior messages in the conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> AwsChatMemory::apply);
    }

    private AwsChatMemory() {
    }

    private static ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(AwsChatMemoryUtils.getChatMemoryRepository(connectionParameters))
            .build();

        return new ChatMemoryFunction.Result(
            MessageChatMemoryAdvisor.builder(chatMemory)
                .build(),
            chatMemory);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/cluster
git commit -m "732 Add AwsChatMemory cluster element"
```

### Task 12: `AwsChatMemoryComponentHandler` + definition snapshot test

**Files:**
- Create: `.../chat-memory-aws/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/AwsChatMemoryComponentHandler.java`
- Create: `.../chat-memory-aws/src/test/java/com/bytechef/component/ai/agent/chat/memory/aws/AwsChatMemoryComponentHandlerTest.java`

- [ ] **Step 1: Write the handler**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.AWS_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.aws.cluster.AwsChatMemory;
import com.bytechef.component.ai.agent.chat.memory.aws.connection.AwsChatMemoryConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class AwsChatMemoryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(AWS_CHAT_MEMORY)
        .title("AWS S3 Chat Memory")
        .description("Stores conversation history as JSON objects in an Amazon S3 bucket.")
        .icon("path:assets/aws-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(AwsChatMemoryConnection.CONNECTION_DEFINITION)
        .clusterElements(AwsChatMemory.of());

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

- [ ] **Step 2: Add a placeholder icon**

Run:
```bash
mkdir -p server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/src/main/resources/assets
cp server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/resources/assets/*.svg \
   server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/src/main/resources/assets/aws-chat-memory.svg
```
Expected: an `aws-chat-memory.svg` exists. (Replace with a proper AWS icon later; any valid SVG keeps the definition test green.)

- [ ] **Step 3: Write the definition snapshot test**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws;

import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.test.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AwsChatMemoryComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/awsChatMemory_v1.json", new AwsChatMemoryComponentHandler()
            .getDefinition());
    }
}
```

Note: if `JsonFileAssert` is not the snapshot helper used by sibling chat-memory components, open
`server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/test/.../JdbcChatMemoryComponentHandlerTest.java`
and copy its exact assertion style and import.

- [ ] **Step 4: Run the test to auto-generate the definition JSON**

Run:
```bash
rm -f server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/src/test/resources/definition/*.json \
      server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws/build/resources/test/definition/*.json 2>/dev/null
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:test
```
Expected: PASS — the JSON is generated on first run, then asserted.

- [ ] **Step 5: Format and commit**

```bash
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:spotlessApply
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws
git commit -m "732 Add AwsChatMemoryComponentHandler + definition test"
```

---

## Phase 4 — Component: `chat-memory-aws-session`

### Task 13: Scaffold module + constants + connection

**Files:**
- Create: `.../chat-memory-aws-session/build.gradle.kts`
- Create: `.../chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/constant/AwsSessionChatMemoryConstants.java`
- Create: `.../chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/connection/AwsSessionChatMemoryConnection.java`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

```kotlin
version = "1.0"

dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-session-aws"))
}
```

- [ ] **Step 2: Register in `settings.gradle.kts`**

```kotlin
include("server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session")
```

- [ ] **Step 3: Write the constants**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session.constant;

/**
 * @author Ivica Cardic
 */
public final class AwsSessionChatMemoryConstants {

    public static final String AWS_SESSION_CHAT_MEMORY = "awsSessionChatMemory";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String REGION = "region";
    public static final String BUCKET = "bucket";
    public static final String KEY_PREFIX = "keyPrefix";

    private AwsSessionChatMemoryConstants() {
    }
}
```

- [ ] **Step 4: Write the connection**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session.connection;

import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public final class AwsSessionChatMemoryConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .required(true)
                        .defaultValue("us-east-1"),
                    string(BUCKET)
                        .label("Bucket")
                        .required(true),
                    string(KEY_PREFIX)
                        .label("Key Prefix")
                        .description("Optional prefix prepended to every session object key.")
                        .required(false)));

    private AwsSessionChatMemoryConnection() {
    }
}
```

- [ ] **Step 5: Verify it compiles + commit**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add settings.gradle.kts server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session
git commit -m "732 Scaffold chat-memory-aws-session component (constants + connection)"
```

### Task 14: `AwsSessionChatMemoryUtils` + `AwsSessionChatMemory` cluster element

**Files:**
- Create: `.../chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/util/AwsSessionChatMemoryUtils.java`
- Create: `.../chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/cluster/AwsSessionChatMemory.java`

- [ ] **Step 1: Write the util**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.SECRET_ACCESS_KEY;

import com.bytechef.component.definition.Parameters;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.s3.S3SessionRepository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public final class AwsSessionChatMemoryUtils {

    private AwsSessionChatMemoryUtils() {
    }

    public static SessionRepository getSessionRepository(Parameters connectionParameters) {
        S3Client s3Client = S3Client.builder()
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY))))
            .build();

        return S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
            .build();
    }
}
```

- [ ] **Step 2: Write the cluster element**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.aws.session.util.AwsSessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import java.util.Map;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public final class AwsSessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of() {
        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("AWS S3 Session Repository")
            .description("Stores session events as JSON objects in an Amazon S3 bucket.")
            .type(SESSION_REPOSITORY)
            .object(() -> AwsSessionChatMemory::apply);
    }

    private AwsSessionChatMemory() {
    }

    private static SessionRepository apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        return AwsSessionChatMemoryUtils.getSessionRepository(connectionParameters);
    }
}
```

- [ ] **Step 3: Verify it compiles + commit**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/util \
        server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/cluster
git commit -m "732 Add AwsSessionChatMemory util + cluster element"
```

### Task 15: `AwsSessionChatMemoryComponentHandler` + definition test

**Files:**
- Create: `.../chat-memory-aws-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/aws/session/AwsSessionChatMemoryComponentHandler.java`
- Create: `.../chat-memory-aws-session/src/test/java/com/bytechef/component/ai/agent/chat/memory/aws/session/AwsSessionChatMemoryComponentHandlerTest.java`

- [ ] **Step 1: Write the handler**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session;

import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.AWS_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.aws.session.cluster.AwsSessionChatMemory;
import com.bytechef.component.ai.agent.chat.memory.aws.session.connection.AwsSessionChatMemoryConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class AwsSessionChatMemoryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(AWS_SESSION_CHAT_MEMORY)
        .title("AWS S3 Session Repository")
        .description("Stores agent session events as JSON objects in an Amazon S3 bucket.")
        .icon("path:assets/aws-session-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(AwsSessionChatMemoryConnection.CONNECTION_DEFINITION)
        .clusterElements(AwsSessionChatMemory.of());

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

- [ ] **Step 2: Add a placeholder icon**

Run:
```bash
mkdir -p server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/src/main/resources/assets
cp server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc-session/src/main/resources/assets/*.svg \
   server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/src/main/resources/assets/aws-session-chat-memory.svg
```
Expected: an `aws-session-chat-memory.svg` exists. (If `chat-memory-jdbc-session` has no asset, copy from `chat-memory-builtin-session`.)

- [ ] **Step 3: Write the definition test**

```java
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

package com.bytechef.component.ai.agent.chat.memory.aws.session;

import com.bytechef.component.test.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AwsSessionChatMemoryComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/awsSessionChatMemory_v1.json", new AwsSessionChatMemoryComponentHandler()
                .getDefinition());
    }
}
```

- [ ] **Step 4: Run the test to generate the definition JSON**

Run:
```bash
rm -f server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/src/test/resources/definition/*.json \
      server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session/build/resources/test/definition/*.json 2>/dev/null
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session:test
```
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session:spotlessApply
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-aws-session
git commit -m "732 Add AwsSessionChatMemoryComponentHandler + definition test"
```

---

## Phase 5 — Built-in chat memory (config-layer extension, bucket-per-tenant)

### Task 16: Scaffold `ai-chat-memory-aws-config` + properties + S3 client factory

**Files:**
- Create: `server/libs/config/ai-chat-memory-config/ai-chat-memory-aws-config/build.gradle.kts`
- Create: `.../ai-chat-memory-aws-config/src/main/java/com/bytechef/ai/chat/memory/aws/config/AwsChatMemoryProperties.java`
- Create: `.../ai-chat-memory-aws-config/src/main/java/com/bytechef/ai/chat/memory/aws/config/AwsS3ClientFactory.java`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("software.amazon.awssdk:s3")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":spring-ai:spring-ai-model-chat-memory-repository-aws"))
}
```

- [ ] **Step 2: Register in `settings.gradle.kts`**

```kotlin
include("server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config")
```

- [ ] **Step 3: Write the properties record**

```java
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

package com.bytechef.ai.chat.memory.aws.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai.memory.aws")
public record AwsChatMemoryProperties(
    String bucketPrefix, @Nullable String region, @Nullable String accessKeyId, @Nullable String secretAccessKey,
    String keyPrefix) {

    public AwsChatMemoryProperties {
        if (bucketPrefix == null || bucketPrefix.isBlank()) {
            bucketPrefix = "bytechef-chat-memory";
        }

        if (keyPrefix == null) {
            keyPrefix = "";
        }
    }
}
```

- [ ] **Step 4: Write the S3 client factory** (explicit creds when present, else default credential chain)

```java
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

package com.bytechef.ai.chat.memory.aws.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
final class AwsS3ClientFactory {

    private AwsS3ClientFactory() {
    }

    static S3Client create(AwsChatMemoryProperties properties) {
        S3Client.Builder builder = S3Client.builder();

        if (properties.region() != null && !properties.region()
            .isBlank()) {
            builder.region(Region.of(properties.region()));
        }

        if (properties.accessKeyId() != null && !properties.accessKeyId()
            .isBlank() && properties.secretAccessKey() != null && !properties.secretAccessKey()
                .isBlank()) {

            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
```

- [ ] **Step 5: Verify + commit**

Run: `./gradlew :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add settings.gradle.kts server/libs/config/ai-chat-memory-config/ai-chat-memory-aws-config
git commit -m "732 Scaffold ai-chat-memory-aws-config (properties + S3 client factory)"
```

### Task 17: `TenantRoutingS3ChatMemoryRepository` (TDD)

**Files:**
- Create: `.../ai-chat-memory-aws-config/src/main/java/com/bytechef/ai/chat/memory/aws/config/TenantRoutingS3ChatMemoryRepository.java`
- Create: `.../ai-chat-memory-aws-config/src/test/java/com/bytechef/ai/chat/memory/aws/config/TenantRoutingS3ChatMemoryRepositoryTest.java`

- [ ] **Step 1: Write the failing test** (verifies the bucket name is derived from `TenantContext` and the per-tenant repo is created against `{prefix}-{tenantId}`)

```java
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

package com.bytechef.ai.chat.memory.aws.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

class TenantRoutingS3ChatMemoryRepositoryTest {

    @Test
    void testFindConversationIdsUsesTenantScopedBucket() {
        S3Client s3Client = Mockito.mock(S3Client.class);

        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder()
            .build());
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder()
            .build());

        TenantRoutingS3ChatMemoryRepository repository = new TenantRoutingS3ChatMemoryRepository(
            s3Client, "bytechef-chat-memory", "");

        TenantContext.setCurrentTenantId("0000000001");

        try {
            repository.findConversationIds();
        } finally {
            TenantContext.resetCurrentTenantId();
        }

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);

        verify(s3Client).listObjectsV2(captor.capture());

        org.junit.jupiter.api.Assertions.assertEquals("bytechef-chat-memory-0000000001", captor.getValue()
            .bucket());
    }
}
```

Note: confirm `TenantContext`'s setter/reset method names in
`server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/TenantContext.java` and adjust the
test's `setCurrentTenantId` / `resetCurrentTenantId` calls to match the real API.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:test --tests '*TenantRoutingS3ChatMemoryRepositoryTest*'`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write the implementation**

```java
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

package com.bytechef.ai.chat.memory.aws.config;

import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.s3.S3ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * A {@link ChatMemoryRepository} that routes every call to a per-tenant {@link S3ChatMemoryRepository} bound to bucket
 * {@code {bucketPrefix}-{tenantId}}. Per-tenant repositories are cached (Caffeine, idle-evicted) and their bucket is
 * created on first use. The tenant is read from {@link TenantContext} on each call, mirroring the per-tenant Caffeine
 * pattern used by {@code InMemorySessionRepositoryHolder}.
 *
 * @author Ivica Cardic
 */
final class TenantRoutingS3ChatMemoryRepository implements ChatMemoryRepository {

    private final Cache<String, S3ChatMemoryRepository> repositories = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();
    private final S3Client s3Client;
    private final String bucketPrefix;
    private final String keyPrefix;

    TenantRoutingS3ChatMemoryRepository(S3Client s3Client, String bucketPrefix, String keyPrefix) {
        this.s3Client = s3Client;
        this.bucketPrefix = bucketPrefix;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public List<String> findConversationIds() {
        return resolve().findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return resolve().findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        resolve().saveAll(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        resolve().deleteByConversationId(conversationId);
    }

    private S3ChatMemoryRepository resolve() {
        return repositories.get(TenantContext.getCurrentTenantId(), this::createForTenant);
    }

    private S3ChatMemoryRepository createForTenant(String tenantId) {
        String bucketName = bucketPrefix + "-" + tenantId;

        ensureBucketExists(bucketName);

        return S3ChatMemoryRepository.builder()
            .s3Client(s3Client)
            .bucketName(bucketName)
            .keyPrefix(keyPrefix)
            .build();
    }

    private void ensureBucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
        } catch (NoSuchBucketException noSuchBucketException) {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:test --tests '*TenantRoutingS3ChatMemoryRepositoryTest*'`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:spotlessApply
git add server/libs/config/ai-chat-memory-config/ai-chat-memory-aws-config/src
git commit -m "732 Add TenantRoutingS3ChatMemoryRepository (bucket-per-tenant)"
```

### Task 18: `AwsChatMemoryConfiguration` bean + autoconfig registration

**Files:**
- Create: `.../ai-chat-memory-aws-config/src/main/java/com/bytechef/ai/chat/memory/aws/config/AwsChatMemoryConfiguration.java`
- Create: `.../ai-chat-memory-aws-config/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write the configuration**

```java
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

package com.bytechef.ai.chat.memory.aws.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableConfigurationProperties(AwsChatMemoryProperties.class)
@ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "aws")
public class AwsChatMemoryConfiguration {

    @Bean
    ChatMemoryRepository awsChatMemoryRepository(AwsChatMemoryProperties properties) {
        S3Client s3Client = AwsS3ClientFactory.create(properties);

        return new TenantRoutingS3ChatMemoryRepository(s3Client, properties.bucketPrefix(), properties.keyPrefix());
    }

    @Bean
    ChatMemory awsChatMemory(ChatMemoryRepository awsChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(awsChatMemoryRepository)
            .maxMessages(500)
            .build();
    }
}
```

- [ ] **Step 2: Register the autoconfiguration**

Create `.../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with exactly:

```
com.bytechef.ai.chat.memory.aws.config.AwsChatMemoryConfiguration
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Wire the module into the server app** so the bean is on the classpath when `provider=aws`.

Find where `ai-chat-memory-jdbc-config` is declared as a dependency:
```bash
grep -rn "ai-chat-memory-jdbc-config" server --include=build.gradle.kts
```
For each `build.gradle.kts` that depends on `ai-chat-memory-jdbc-config`, add an analogous line:
```kotlin
implementation(project(":server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config"))
```

- [ ] **Step 5: Verify the server app still compiles**

Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/libs/config/ai-chat-memory-config/ai-chat-memory-aws-config/src/main \
        $(grep -rl "ai-chat-memory-aws-config" server --include=build.gradle.kts)
git commit -m "732 Wire AwsChatMemoryConfiguration as built-in chat memory provider=aws"
```

---

## Phase 6 — Built-in session (extend the existing handler, bucket-per-tenant)

### Task 19: `BuiltInSessionProperties` + `TenantRoutingS3SessionRepository`

**Files:**
- Modify: `server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session/build.gradle.kts`
- Create: `.../chat-memory-builtin-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/builtin/session/util/TenantRoutingS3SessionRepository.java`

- [ ] **Step 1: Add dependencies to the module build file**

Open `chat-memory-builtin-session/build.gradle.kts` and add to the `dependencies { ... }` block (keep existing entries):

```kotlin
    implementation("software.amazon.awssdk:s3")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":spring-ai:spring-ai-session-aws"))
```

- [ ] **Step 2: Write `TenantRoutingS3SessionRepository`** (full delegate over the `SessionRepository` contract)

```java
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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.s3.S3SessionRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * A {@link SessionRepository} that routes every call to a per-tenant {@link S3SessionRepository} bound to bucket
 * {@code {bucketPrefix}-{tenantId}}, mirroring {@code InMemorySessionRepositoryHolder}'s per-tenant Caffeine pattern.
 *
 * @author Ivica Cardic
 */
public final class TenantRoutingS3SessionRepository implements SessionRepository {

    private final Cache<String, S3SessionRepository> repositories = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();
    private final S3Client s3Client;
    private final String bucketPrefix;
    private final String keyPrefix;

    public TenantRoutingS3SessionRepository(S3Client s3Client, String bucketPrefix, String keyPrefix) {
        this.s3Client = s3Client;
        this.bucketPrefix = bucketPrefix;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Session save(Session session) {
        return resolve().save(session);
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        return resolve().findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return resolve().findByUserId(userId);
    }

    @Override
    public List<String> findExpiredSessionIds(Instant before) {
        return resolve().findExpiredSessionIds(before);
    }

    @Override
    public void delete(String sessionId) {
        resolve().delete(sessionId);
    }

    @Override
    public void appendEvent(SessionEvent event) {
        resolve().appendEvent(event);
    }

    @Override
    public void replaceEvents(String sessionId, List<SessionEvent> events) {
        resolve().replaceEvents(sessionId, events);
    }

    @Override
    public boolean replaceEvents(String sessionId, List<SessionEvent> events, long expectedVersion) {
        return resolve().replaceEvents(sessionId, events, expectedVersion);
    }

    @Override
    public long getEventVersion(String sessionId) {
        return resolve().getEventVersion(sessionId);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        return resolve().findEvents(sessionId, filter);
    }

    private S3SessionRepository resolve() {
        return repositories.get(TenantContext.getCurrentTenantId(), this::createForTenant);
    }

    private S3SessionRepository createForTenant(String tenantId) {
        String bucketName = bucketPrefix + "-" + tenantId;

        ensureBucketExists(bucketName);

        return S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(bucketName)
            .keyPrefix(keyPrefix)
            .build();
    }

    private void ensureBucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
        } catch (NoSuchBucketException noSuchBucketException) {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session/build.gradle.kts \
        server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/builtin/session/util/TenantRoutingS3SessionRepository.java
git commit -m "732 Add TenantRoutingS3SessionRepository to built-in session module"
```

### Task 20: Extend `BuiltInSessionChatMemory` to select S3 via `bytechef.ai.session.provider`

**Files:**
- Modify: `.../chat-memory-builtin-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/builtin/session/cluster/BuiltInSessionChatMemory.java`
- Modify: `.../chat-memory-builtin-session/src/main/java/com/bytechef/component/ai/agent/chat/memory/builtin/session/BuiltInSessionChatMemoryComponentHandler.java`

The current `BuiltInSessionChatMemory.of(JdbcTemplate)` chooses JDBC-vs-in-memory. Extend it with an
explicit S3 path; the handler reads the provider + S3 settings from the Spring `Environment` and passes a
ready-built `S3Client` (or null) so the cluster class stays free of AWS-config plumbing.

- [ ] **Step 1: Replace `BuiltInSessionChatMemory.java` with the extended version**

```java
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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.TenantRoutingS3SessionRepository;
import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public class BuiltInSessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of(@Nullable JdbcTemplate jdbcTemplate) {
        return of(jdbcTemplate, null, null, "");
    }

    public static ClusterElementDefinition<SessionRepositoryFunction> of(
        @Nullable JdbcTemplate jdbcTemplate, @Nullable S3Client s3Client, @Nullable String bucketPrefix,
        String keyPrefix) {

        SessionRepository sessionRepository = resolve(jdbcTemplate, s3Client, bucketPrefix, keyPrefix);

        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("Built-in Session Repository")
            .description("Stores session events in the application's configured backend.")
            .type(SESSION_REPOSITORY)
            .object(
                () -> (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository);
    }

    private BuiltInSessionChatMemory() {
    }

    private static SessionRepository resolve(
        @Nullable JdbcTemplate jdbcTemplate, @Nullable S3Client s3Client, @Nullable String bucketPrefix,
        String keyPrefix) {

        if (s3Client != null && bucketPrefix != null) {
            return new TenantRoutingS3SessionRepository(s3Client, bucketPrefix, keyPrefix);
        }

        DataSource dataSource = jdbcTemplate == null ? null : jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return InMemorySessionRepository.builder()
                .build();
        }

        return SessionChatMemoryUtils.getSessionRepository(dataSource);
    }
}
```

Note: this preserves the original single-arg `of(JdbcTemplate)` signature (so existing callers keep working)
and adds the four-arg overload. Match the file's existing import style for `SessionRepositoryFunction`.

- [ ] **Step 2: Update the handler to build the S3 client from the environment**

Replace `BuiltInSessionChatMemoryComponentHandler.java` with:

```java
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

package com.bytechef.component.ai.agent.chat.memory.builtin.session;

import static com.bytechef.component.ai.agent.chat.memory.builtin.session.constant.BuiltInSessionChatMemoryConstants.BUILT_IN_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster.BuiltInSessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
@Component(BUILT_IN_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class BuiltInSessionChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public BuiltInSessionChatMemoryComponentHandler(
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate, Environment environment) {

        boolean awsProvider = "aws".equals(environment.getProperty("bytechef.ai.session.provider"));

        S3Client s3Client = awsProvider ? buildS3Client(environment) : null;
        String bucketPrefix = awsProvider
            ? environment.getProperty("bytechef.ai.session.aws.bucket-prefix", "bytechef-session")
            : null;
        String keyPrefix = environment.getProperty("bytechef.ai.session.aws.key-prefix", "");

        this.componentDefinition = component(BUILT_IN_SESSION_CHAT_MEMORY)
            .title("Built-in Session Repository")
            .description("Built-in storage backend for Session Chat Memory.")
            .icon("path:assets/built-in-session-chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(BuiltInSessionChatMemory.of(jdbcTemplate, s3Client, bucketPrefix, keyPrefix));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static S3Client buildS3Client(Environment environment) {
        S3Client.Builder builder = S3Client.builder();

        String region = environment.getProperty("bytechef.ai.session.aws.region");

        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }

        String accessKeyId = environment.getProperty("bytechef.ai.session.aws.access-key-id");
        String secretAccessKey = environment.getProperty("bytechef.ai.session.aws.secret-access-key");

        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Update the existing handler test**

Open `BuiltInSessionChatMemoryComponentHandlerTest.java`. The constructor now requires an `Environment`.
Update the construction to pass a stub environment with the default (non-AWS) provider so existing behavior
is preserved:

```java
new BuiltInSessionChatMemoryComponentHandler(null, new org.springframework.mock.env.MockEnvironment());
```

(`MockEnvironment` comes from `spring-test`/`spring-core` test scope — confirm it resolves; if not, use
`new org.springframework.core.env.StandardEnvironment()`.)

- [ ] **Step 5: Run the module tests**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:test`
Expected: PASS.

- [ ] **Step 6: Format and commit**

```bash
./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:spotlessApply
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-builtin-session/src
git commit -m "732 Extend built-in session repository with AWS S3 provider"
```

---

## Phase 7 — Whole-project verification

### Task 21: Build, format, and full check

- [ ] **Step 1: Spotless across the repo**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL (applies/validates the Apache header on all new files — none should get the EE header since none carry `@version ee`).

- [ ] **Step 2: Compile everything**

Run: `./gradlew clean compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the new modules' tests** (Docker required for the two LocalStack int tests)

Run:
```bash
./gradlew :spring-ai:spring-ai-model-chat-memory-repository-aws:test \
          :spring-ai:spring-ai-session-aws:test \
          :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws:test \
          :server:libs:modules:components:ai:agent:chat-memory:chat-memory-aws-session:test \
          :server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config:test \
          :server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session:test
```
Expected: PASS.

- [ ] **Step 4: Final commit if spotless changed anything**

```bash
git add -A
git commit -m "732 Apply spotless formatting for AWS S3 chat memory and session" || echo "nothing to commit"
```

---

## Self-Review Notes (for the implementing agent)

- **Verify before coding:** the exact `TenantContext` API (`getCurrentTenantId` / setter / reset), the snapshot-test helper class used by sibling chat-memory component tests (the plan assumes `JsonFileAssert` — copy the real one), and the `Parameters.getString(name, default)` / `getRequiredString(name)` method names on `com.bytechef.component.definition.Parameters`.
- **AWS SDK conditional write:** if `PutObjectRequest.ifMatch(...)` is unavailable in the pinned SDK version, upgrade the `software.amazon.awssdk:s3` BOM or fall back to version-field-only CAS (read version, compare, write) — note this trades true atomicity for a small race window and document it.
- **LocalStack image tag:** `localstack/localstack:3.5` is a known-good tag; bump if the test infra pins another.
- **Region for built-in:** when `region` is blank and using the default credential chain, the SDK resolves region from the environment/instance profile — document that operators must set `AWS_REGION` or `bytechef.ai.{memory,session}.aws.region`.
