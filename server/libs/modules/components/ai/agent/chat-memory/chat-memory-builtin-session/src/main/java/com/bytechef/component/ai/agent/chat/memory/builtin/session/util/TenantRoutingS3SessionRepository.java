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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Tenant-routing {@link SessionRepository} backed by AWS S3.
 *
 * <p>
 * Each tenant gets its own S3 bucket: {@code {bucketPrefix}-{tenantId}}. The bucket is created on first use if it does
 * not exist. Per-tenant {@link S3SessionRepository} instances are cached in a Caffeine cache with a 1-hour
 * expiry-after-access policy so idle tenants don't pin memory indefinitely.
 *
 * @author Ivica Cardic
 */
public final class TenantRoutingS3SessionRepository implements SessionRepository {

    private final Cache<String, SessionRepository> repositories = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    private final S3Client s3Client;
    private final String bucketPrefix;
    private final String keyPrefix;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
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

    private SessionRepository resolve() {
        return repositories.get(TenantContext.getCurrentTenantId(), this::createForTenant);
    }

    private SessionRepository createForTenant(String tenantId) {
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
