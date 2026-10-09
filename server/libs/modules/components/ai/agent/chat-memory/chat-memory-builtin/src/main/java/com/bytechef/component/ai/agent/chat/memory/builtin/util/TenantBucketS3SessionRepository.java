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

package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.s3.S3SessionRepository;
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
 * @author Ivica Cardic
 */
final class TenantBucketS3SessionRepository implements SessionRepository {

    private static final Logger log = LoggerFactory.getLogger(TenantBucketS3SessionRepository.class);

    private static final int BUCKET_NAME_HASH_LENGTH = 16;
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final Pattern INVALID_BUCKET_NAME_CHARACTERS = Pattern.compile("[^a-z0-9-]+");
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int MAX_BUCKET_NAME_LENGTH = 63;
    private static final Pattern VALID_BUCKET_NAME = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    private final String bucketName;
    private final S3Client s3Client;
    private final SessionRepository sessionRepository;
    private volatile boolean bucketAvailable;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    TenantBucketS3SessionRepository(S3Client s3Client, String bucketName, String keyPrefix, int maxArchivedEvents) {
        this.bucketName = bucketName;
        this.s3Client = s3Client;
        this.sessionRepository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(bucketName)
            .keyPrefix(keyPrefix)
            .maxArchivedEvents(maxArchivedEvents)
            .build();
    }

    static String toBucketName(String bucketPrefix, String tenantId) {
        String bucketName = bucketPrefix + "-" + tenantId;

        Matcher validBucketNameMatcher = VALID_BUCKET_NAME.matcher(bucketName);

        if (validBucketNameMatcher.matches()) {
            return bucketName;
        }

        String hash = HexFormat.of()
            .formatHex(sha256(bucketName))
            .substring(0, BUCKET_NAME_HASH_LENGTH);

        String sanitizedBucketName = INVALID_BUCKET_NAME_CHARACTERS.matcher(bucketName.toLowerCase(Locale.ROOT))
            .replaceAll("-");

        int maxBaseLength = MAX_BUCKET_NAME_LENGTH - BUCKET_NAME_HASH_LENGTH - 1;

        if (sanitizedBucketName.length() > maxBaseLength) {
            sanitizedBucketName = sanitizedBucketName.substring(0, maxBaseLength);
        }

        sanitizedBucketName = EDGE_HYPHENS.matcher(sanitizedBucketName)
            .replaceAll("");

        return sanitizedBucketName.isEmpty() ? hash : sanitizedBucketName + "-" + hash;
    }

    @Override
    public Session save(Session session) {
        requireBucket();

        return sessionRepository.save(session);
    }

    @Override
    public boolean saveIfAbsent(Session session) {
        requireBucket();

        return sessionRepository.saveIfAbsent(session);
    }

    @Override
    @Nullable
    public Session findById(String sessionId) {
        if (!isBucketAvailable()) {
            return null;
        }

        return sessionRepository.findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        if (!isBucketAvailable()) {
            return List.of();
        }

        return sessionRepository.findByUserId(userId);
    }

    @Override
    public int deleteExpiredSessions(Instant before) {
        if (!isBucketAvailable()) {
            return 0;
        }

        return sessionRepository.deleteExpiredSessions(before);
    }

    @Override
    public void delete(String sessionId) {
        if (isBucketAvailable()) {
            sessionRepository.delete(sessionId);
        }
    }

    @Override
    public void appendEvent(SessionEvent event) {
        requireBucket();

        sessionRepository.appendEvent(event);
    }

    @Override
    public boolean applyCompaction(String sessionId, CompactionPlan plan, long expectedVersion) {
        requireBucket();

        return sessionRepository.applyCompaction(sessionId, plan, expectedVersion);
    }

    @Override
    public long getEventVersion(String sessionId) {
        if (!isBucketAvailable()) {
            return 0L;
        }

        return sessionRepository.getEventVersion(sessionId);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        if (!isBucketAvailable()) {
            return List.of();
        }

        return sessionRepository.findEvents(sessionId, filter);
    }

    @Override
    public List<SessionEvent> findEventsByUserId(String userId, EventFilter filter) {
        if (!isBucketAvailable()) {
            return List.of();
        }

        return sessionRepository.findEventsByUserId(userId, filter);
    }

    private boolean isBucketAvailable() {
        if (!bucketAvailable && bucketExists()) {
            bucketAvailable = true;
        }

        return bucketAvailable;
    }

    private void requireBucket() {
        if (isBucketAvailable()) {
            return;
        }

        createBucket();

        bucketAvailable = true;
    }

    private boolean bucketExists() {
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
                        "bytechef.ai.memory.aws.session-bucket-prefix",
                    s3Exception);
            }

            throw s3Exception;
        }
    }

    private void createBucket() {
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
                    "bytechef.ai.memory.aws.session-bucket-prefix",
                bucketAlreadyExistsException);
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() == HTTP_FORBIDDEN) {
                throw new IllegalStateException(
                    "S3 bucket " + bucketName + " cannot be created: access denied (the name is owned by another " +
                        "AWS account or the credentials lack s3:CreateBucket); configure a unique " +
                        "bytechef.ai.memory.aws.session-bucket-prefix",
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

    private static byte[] sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is not available", noSuchAlgorithmException);
        }
    }
}
