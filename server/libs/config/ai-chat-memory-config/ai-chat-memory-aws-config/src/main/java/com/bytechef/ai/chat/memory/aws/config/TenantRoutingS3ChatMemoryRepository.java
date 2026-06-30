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
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Tenant-routing {@link ChatMemoryRepository} backed by AWS S3.
 *
 * <p>
 * Each tenant gets its own S3 bucket: {@code {bucketPrefix}-{tenantId}}. The bucket is created on first use if it does
 * not exist. Per-tenant {@link S3ChatMemoryRepository} instances are cached in a Caffeine cache with a 1-hour
 * expiry-after-access policy so idle tenants don't pin memory indefinitely.
 *
 * @author Ivica Cardic
 */
final class TenantRoutingS3ChatMemoryRepository implements ChatMemoryRepository {

    private final Cache<String, ChatMemoryRepository> repositories = Caffeine.newBuilder()
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

    private ChatMemoryRepository resolve() {
        return repositories.get(TenantContext.getCurrentTenantId(), this::createForTenant);
    }

    private ChatMemoryRepository createForTenant(String tenantId) {
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

            return;
        } catch (NoSuchBucketException ignored) {
            // bucket missing — create it below
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() != 404) {
                throw s3Exception;
            }
            // a missing bucket can surface as a generic 404 S3Exception rather than NoSuchBucketException — create
            // below
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ignored) {
            // another pod created it concurrently — bucket is ready
        }
    }
}
