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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.store.AbstractDocumentSessionRepository;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.StoredSession;
import org.springframework.util.Assert;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
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
 * @author Ivica Cardic
 */
public final class S3SessionRepository extends AbstractDocumentSessionRepository<String> {

    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_PRECONDITION_FAILED = 412;
    private static final String NO_SUCH_KEY_ERROR_CODE = "NoSuchKey";
    private static final String SUFFIX = ".json";

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;

    private S3SessionRepository(Builder builder) {
        super(builder.jsonMapper, builder.conditionalWriteRetry, builder.maxArchivedEvents);

        this.s3Client = builder.s3Client;
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
    }

    @Override
    protected String documentKey(String sessionId) {
        return keyPrefix + sessionId + SUFFIX;
    }

    @Override
    protected List<String> listDocumentKeys() {
        List<String> documentKeys = new ArrayList<>();

        for (S3Object s3Object : s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(keyPrefix)
            .build())
            .contents()) {

            String objectKey = s3Object.key();

            if (objectKey.endsWith(SUFFIX)) {
                documentKeys.add(objectKey);
            }
        }

        return documentKeys;
    }

    @Override
    @Nullable
    protected LoadedSession<String> loadDocument(String documentKey) {
        ResponseBytes<GetObjectResponse> response;

        try {
            response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(documentKey)
                    .build());
        } catch (NoSuchKeyException noSuchKeyException) {
            return null;
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() == HTTP_FORBIDDEN) {
                throw new IllegalStateException(
                    "Access denied reading session object s3://" + bucketName + "/" + documentKey +
                        "; the credentials may lack s3:GetObject on the object, kms:Decrypt on its KMS key, or " +
                        "s3:ListBucket on bucket " + bucketName + " (without it S3 reports a missing object as 403)",
                    s3Exception);
            }

            throw s3Exception;
        }

        GetObjectResponse getObjectResponse = response.response();

        String eTag = getObjectResponse.eTag();

        if (eTag == null) {
            throw new IllegalStateException(
                "S3 returned no ETag for session object s3://" + bucketName + "/" + documentKey +
                    "; conditional writes require one");
        }

        StoredSession document = jsonMapper().readValue(response.asByteArray(), StoredSession.class);

        return new LoadedSession<>(document, eTag);
    }

    @Override
    protected boolean tryPut(StoredSession document, @Nullable LoadedSession<String> current) {
        String ifMatchEtag = current == null ? null : current.casToken();

        try {
            put(document, ifMatchEtag);

            return true;
        } catch (S3Exception s3Exception) {
            if (isConditionalWriteConflict(s3Exception) || (ifMatchEtag != null && isNoSuchKey(s3Exception))) {

                return false;
            }

            throw s3Exception;
        }
    }

    @Override
    protected CreateResult tryCreate(StoredSession document) {
        try {
            put(document, null);

            return CreateResult.CREATED;
        } catch (S3Exception s3Exception) {
            int statusCode = s3Exception.statusCode();

            if (statusCode == HTTP_PRECONDITION_FAILED) {
                return CreateResult.ALREADY_EXISTS;
            }

            if (statusCode == HTTP_CONFLICT) {
                return CreateResult.CONFLICT;
            }

            throw s3Exception;
        }
    }

    @Override
    protected boolean tryDeleteIfExpired(LoadedSession<String> loadedSession, Instant before) {
        StoredSession document = loadedSession.document();

        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(documentKey(document.id()))
            .ifMatch(loadedSession.casToken())
            .build();

        try {
            s3Client.deleteObject(request);

            return true;
        } catch (S3Exception s3Exception) {
            if (isConditionalWriteConflict(s3Exception) || isNoSuchKey(s3Exception)) {
                return false;
            }

            throw s3Exception;
        }
    }

    @Override
    protected void deleteDocument(String documentKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(documentKey)
            .build());
    }

    private void put(StoredSession document, @Nullable String ifMatchEtag) {
        PutObjectRequest.Builder request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(documentKey(document.id()))
            .contentType("application/json");

        if (ifMatchEtag != null) {
            request.ifMatch(ifMatchEtag);
        } else {
            request.ifNoneMatch("*");
        }

        s3Client.putObject(request.build(), RequestBody.fromBytes(jsonMapper().writeValueAsBytes(document)));
    }

    private static boolean isNoSuchKey(S3Exception s3Exception) {
        if (s3Exception instanceof NoSuchKeyException) {
            return true;
        }

        AwsErrorDetails awsErrorDetails = s3Exception.awsErrorDetails();

        return s3Exception.statusCode() == HTTP_NOT_FOUND && awsErrorDetails != null &&
            NO_SUCH_KEY_ERROR_CODE.equals(awsErrorDetails.errorCode());
    }

    private static boolean isConditionalWriteConflict(S3Exception s3Exception) {
        int statusCode = s3Exception.statusCode();

        return statusCode == HTTP_PRECONDITION_FAILED || statusCode == HTTP_CONFLICT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private S3Client s3Client;
        private String bucketName;
        private String keyPrefix = "";
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();
        private ConditionalWriteRetry conditionalWriteRetry = ConditionalWriteRetry.defaults();
        private int maxArchivedEvents = StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS;

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

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder jsonMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;

            return this;
        }

        public Builder maxArchivedEvents(int maxArchivedEvents) {
            this.maxArchivedEvents = maxArchivedEvents;

            return this;
        }

        Builder conditionalWriteRetry(ConditionalWriteRetry conditionalWriteRetry) {
            this.conditionalWriteRetry = conditionalWriteRetry;

            return this;
        }

        public S3SessionRepository build() {
            Assert.notNull(s3Client, "s3Client must not be null");
            Assert.hasText(bucketName, "bucketName must not be null or empty");
            Assert.notNull(keyPrefix, "keyPrefix must not be null");
            Assert.notNull(jsonMapper, "jsonMapper must not be null");
            Assert.notNull(conditionalWriteRetry, "conditionalWriteRetry must not be null");
            Assert.isTrue(maxArchivedEvents >= 0, "maxArchivedEvents must not be negative");

            return new S3SessionRepository(this);
        }
    }
}
