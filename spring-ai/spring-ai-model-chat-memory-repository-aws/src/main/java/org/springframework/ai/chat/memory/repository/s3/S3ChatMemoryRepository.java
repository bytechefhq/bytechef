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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
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
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    private S3ChatMemoryRepository(Builder builder) {
        this.s3Client = builder.s3Client;
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
    }

    @Override
    public List<String> findConversationIds() {
        List<S3Object> objects = new ArrayList<>();

        ListObjectsV2Iterable listObjectsV2Responses = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(keyPrefix)
            .build());

        for (S3Object s3Object : listObjectsV2Responses.contents()) {
            objects.add(s3Object);
        }

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
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key(conversationId))
                .build();

            response = s3Client.getObjectAsBytes(getObjectRequest);
        } catch (NoSuchKeyException noSuchKeyException) {
            return List.of();
        }

        List<StoredMessage> stored = jsonMapper.readValue(
            response.asByteArray(), new TypeReference<>() {});

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

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key(conversationId))
            .contentType("application/json")
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(body));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key(conversationId))
            .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private String key(String conversationId) {
        return keyPrefix + conversationId + SUFFIX;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private S3Client s3Client;
        private String bucketName;
        private String keyPrefix = "";

        private Builder() {
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
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

        public S3ChatMemoryRepository build() {
            Assert.notNull(s3Client, "s3Client must not be null");
            Assert.hasText(bucketName, "bucketName must not be null or empty");

            return new S3ChatMemoryRepository(this);
        }
    }
}
