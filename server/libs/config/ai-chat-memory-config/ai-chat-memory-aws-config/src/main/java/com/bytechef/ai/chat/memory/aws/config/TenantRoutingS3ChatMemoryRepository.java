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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.s3.S3ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
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

    private static final Logger log = LoggerFactory.getLogger(TenantRoutingS3ChatMemoryRepository.class);

    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;

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
        if (bucketExists(bucketName)) {
            return;
        }

        CreateBucketRequest.Builder createBucketRequestBuilder = CreateBucketRequest.builder()
            .bucket(bucketName);

        Region region = getRegion();

        if (region != null && !Region.US_EAST_1.equals(region)) {
            createBucketRequestBuilder.createBucketConfiguration(CreateBucketConfiguration.builder()
                .locationConstraint(region.id())
                .build());
        }

        try {
            s3Client.createBucket(createBucketRequestBuilder.build());
        } catch (BucketAlreadyOwnedByYouException bucketAlreadyOwnedByYouException) {
            log.debug("S3 bucket {} was created concurrently", bucketName, bucketAlreadyOwnedByYouException);
        } catch (BucketAlreadyExistsException bucketAlreadyExistsException) {
            throw new IllegalStateException(
                "S3 bucket " + bucketName + " is owned by another AWS account; configure a unique " +
                    "bytechef.ai.memory.aws.bucket-prefix",
                bucketAlreadyExistsException);
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() == HTTP_FORBIDDEN) {
                throw new IllegalStateException(
                    "S3 bucket " + bucketName + " cannot be created: access denied (the name is owned by another " +
                        "AWS account or the credentials lack s3:CreateBucket); configure a unique " +
                        "bytechef.ai.memory.aws.bucket-prefix",
                    s3Exception);
            }

            throw s3Exception;
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());

            return true;
        } catch (S3Exception s3Exception) {
            if (s3Exception instanceof NoSuchBucketException || s3Exception.statusCode() == HTTP_NOT_FOUND) {
                return false;
            }

            if (s3Exception.statusCode() == HTTP_FORBIDDEN) {
                throw new IllegalStateException(
                    "S3 bucket " + bucketName + " exists but is not accessible (it is owned by another AWS account " +
                        "or the credentials lack s3:ListBucket); configure a unique " +
                        "bytechef.ai.memory.aws.bucket-prefix",
                    s3Exception);
            }

            throw s3Exception;
        }
    }

    @Nullable
    private Region getRegion() {
        S3ServiceClientConfiguration s3ServiceClientConfiguration = s3Client.serviceClientConfiguration();

        return s3ServiceClientConfiguration == null ? null : s3ServiceClientConfiguration.region();
    }
}
