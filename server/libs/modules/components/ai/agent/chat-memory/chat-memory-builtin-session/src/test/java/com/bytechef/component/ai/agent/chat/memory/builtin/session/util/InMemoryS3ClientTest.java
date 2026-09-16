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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author Ivica Cardic
 */
class InMemoryS3ClientTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "session-1.json";

    private InMemoryS3Client inMemoryS3Client;
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        inMemoryS3Client = new InMemoryS3Client();

        inMemoryS3Client.addBucket(BUCKET);

        s3Client = inMemoryS3Client.getS3Client();
    }

    @Test
    void testPutWithIfNoneMatchFailsWhenTheObjectExists() {
        put(PutObjectRequest.builder()
            .ifNoneMatch("*"), "first");

        assertThatThrownBy(() -> put(PutObjectRequest.builder()
            .ifNoneMatch("*"), "second"))
                .isInstanceOfSatisfying(
                    S3Exception.class, s3Exception -> assertThat(s3Exception.statusCode()).isEqualTo(412));
        assertThat(getContent()).isEqualTo("first");
    }

    @Test
    void testPutWithStaleIfMatchFails() {
        put(PutObjectRequest.builder(), "first");

        String staleETag = getETag();

        put(PutObjectRequest.builder()
            .ifMatch(staleETag), "second");

        assertThatThrownBy(() -> put(PutObjectRequest.builder()
            .ifMatch(staleETag), "third"))
                .isInstanceOfSatisfying(
                    S3Exception.class, s3Exception -> assertThat(s3Exception.statusCode()).isEqualTo(412));
        assertThat(getContent()).isEqualTo("second");
    }

    @Test
    void testPutWithIfMatchOnMissingObjectFailsWithNoSuchKey() {
        assertThatThrownBy(() -> put(PutObjectRequest.builder()
            .ifMatch("\"1\""), "first"))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void testDeleteWithStaleIfMatchFails() {
        put(PutObjectRequest.builder(), "first");

        String staleETag = getETag();

        put(PutObjectRequest.builder(), "second");

        assertThatThrownBy(() -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET)
            .key(KEY)
            .ifMatch(staleETag)
            .build()))
                .isInstanceOfSatisfying(
                    S3Exception.class, s3Exception -> assertThat(s3Exception.statusCode()).isEqualTo(412));
        assertThat(inMemoryS3Client.getObjectKeys(BUCKET)).containsExactly(KEY);
    }

    @Test
    void testDeleteWithCurrentIfMatchRemovesTheObject() {
        put(PutObjectRequest.builder(), "first");

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET)
            .key(KEY)
            .ifMatch(getETag())
            .build());

        assertThat(inMemoryS3Client.getObjectKeys(BUCKET)).isEmpty();
    }

    @Test
    void testRewritingIdenticalContentProducesANewETag() {
        put(PutObjectRequest.builder(), "same");

        String firstETag = getETag();

        put(PutObjectRequest.builder(), "same");

        assertThat(getETag()).isNotEqualTo(firstETag);
    }

    private void put(PutObjectRequest.Builder putObjectRequestBuilder, String content) {
        PutObjectRequest putObjectRequest = putObjectRequestBuilder.bucket(BUCKET)
            .key(KEY)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
    }

    private String getContent() {
        ResponseBytes<GetObjectResponse> responseBytes = getObject();

        return responseBytes.asUtf8String();
    }

    private String getETag() {
        ResponseBytes<GetObjectResponse> responseBytes = getObject();

        GetObjectResponse getObjectResponse = responseBytes.response();

        return getObjectResponse.eTag();
    }

    private ResponseBytes<GetObjectResponse> getObject() {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
            .bucket(BUCKET)
            .key(KEY)
            .build());
    }
}
