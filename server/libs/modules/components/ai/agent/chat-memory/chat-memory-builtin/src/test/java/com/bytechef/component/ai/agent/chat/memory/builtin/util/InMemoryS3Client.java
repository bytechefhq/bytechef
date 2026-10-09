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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * @author Ivica Cardic
 */
final class InMemoryS3Client {

    private final Set<String> bucketNames = ConcurrentHashMap.newKeySet();
    private final AtomicLong eTagCounter = new AtomicLong();
    private final Map<String, Map<String, StoredObject>> objectsByBucketName = new ConcurrentHashMap<>();
    private final S3Client s3Client = mock(S3Client.class);

    InMemoryS3Client() {
        when(s3Client.serviceClientConfiguration())
            .thenReturn(S3ServiceClientConfiguration.builder()
                .region(Region.US_EAST_1)
                .build());
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenAnswer(invocation -> headBucket(invocation.getArgument(0)));
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
            .thenAnswer(invocation -> createBucket(invocation.getArgument(0)));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenAnswer(invocation -> getObjectAsBytes(invocation.getArgument(0)));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenAnswer(invocation -> putObject(invocation.getArgument(0), invocation.getArgument(1)));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenAnswer(invocation -> deleteObject(invocation.getArgument(0)));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenAnswer(invocation -> listObjects(invocation.getArgument(0)));
    }

    void addBucket(String bucketName) {
        bucketNames.add(bucketName);
    }

    S3Client getS3Client() {
        return s3Client;
    }

    Set<String> getObjectKeys(String bucketName) {
        return Set.copyOf(getObjects(bucketName).keySet());
    }

    private HeadBucketResponse headBucket(HeadBucketRequest headBucketRequest) {
        requireBucket(headBucketRequest.bucket());

        return HeadBucketResponse.builder()
            .build();
    }

    private CreateBucketResponse createBucket(CreateBucketRequest createBucketRequest) {
        bucketNames.add(createBucketRequest.bucket());

        return CreateBucketResponse.builder()
            .build();
    }

    private ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest getObjectRequest) {
        requireBucket(getObjectRequest.bucket());

        Map<String, StoredObject> objects = getObjects(getObjectRequest.bucket());

        StoredObject storedObject = objects.get(getObjectRequest.key());

        if (storedObject == null) {
            throw noSuchKey();
        }

        GetObjectResponse getObjectResponse = GetObjectResponse.builder()
            .eTag(storedObject.eTag())
            .build();

        return ResponseBytes.fromByteArray(getObjectResponse, storedObject.content());
    }

    private PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) throws Exception {
        requireBucket(putObjectRequest.bucket());

        ContentStreamProvider contentStreamProvider = requestBody.contentStreamProvider();

        byte[] content;

        try (InputStream inputStream = contentStreamProvider.newStream()) {
            content = inputStream.readAllBytes();
        }

        Map<String, StoredObject> objects = getObjects(putObjectRequest.bucket());

        String eTag = "\"" + eTagCounter.incrementAndGet() + "\"";

        objects.compute(putObjectRequest.key(), (key, storedObject) -> {
            if ("*".equals(putObjectRequest.ifNoneMatch()) && storedObject != null) {
                throw preconditionFailed();
            }

            requireMatchingETag(putObjectRequest.ifMatch(), storedObject);

            return new StoredObject(content, eTag);
        });

        return PutObjectResponse.builder()
            .eTag(eTag)
            .build();
    }

    private DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        requireBucket(deleteObjectRequest.bucket());

        Map<String, StoredObject> objects = getObjects(deleteObjectRequest.bucket());

        objects.compute(deleteObjectRequest.key(), (key, storedObject) -> {
            requireMatchingETag(deleteObjectRequest.ifMatch(), storedObject);

            return null;
        });

        return DeleteObjectResponse.builder()
            .build();
    }

    private ListObjectsV2Iterable listObjects(ListObjectsV2Request listObjectsV2Request) {
        requireBucket(listObjectsV2Request.bucket());

        List<S3Object> s3Objects = getObjects(listObjectsV2Request.bucket())
            .keySet()
            .stream()
            .filter(key -> key.startsWith(listObjectsV2Request.prefix()))
            .map(key -> S3Object.builder()
                .key(key)
                .build())
            .toList();

        SdkIterable<S3Object> contents = s3Objects::iterator;

        ListObjectsV2Iterable listObjectsV2Iterable = mock(ListObjectsV2Iterable.class);

        when(listObjectsV2Iterable.contents()).thenReturn(contents);

        return listObjectsV2Iterable;
    }

    private static void requireMatchingETag(@Nullable String ifMatch, @Nullable StoredObject storedObject) {
        if (ifMatch == null) {
            return;
        }

        if (storedObject == null) {
            throw noSuchKey();
        }

        if (!ifMatch.equals(storedObject.eTag())) {
            throw preconditionFailed();
        }
    }

    private static S3Exception noSuchKey() {
        return (S3Exception) NoSuchKeyException.builder()
            .statusCode(404)
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode("NoSuchKey")
                .build())
            .build();
    }

    private static S3Exception preconditionFailed() {
        return (S3Exception) S3Exception.builder()
            .statusCode(412)
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode("PreconditionFailed")
                .build())
            .build();
    }

    private Map<String, StoredObject> getObjects(String bucketName) {
        return objectsByBucketName.computeIfAbsent(bucketName, key -> new ConcurrentHashMap<>());
    }

    private void requireBucket(String bucketName) {
        if (!bucketNames.contains(bucketName)) {
            throw NoSuchBucketException.builder()
                .statusCode(404)
                .build();
        }
    }

    private record StoredObject(byte[] content, String eTag) {
    }
}
