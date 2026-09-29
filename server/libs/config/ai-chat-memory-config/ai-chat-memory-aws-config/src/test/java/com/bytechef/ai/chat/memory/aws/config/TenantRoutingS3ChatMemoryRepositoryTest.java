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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class TenantRoutingS3ChatMemoryRepositoryTest {

    @Mock
    private S3Client s3Client;

    @Test
    void testDeleteByConversationIdUsesTenantBucketName() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder()
            .build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder()
            .build());

        TenantRoutingS3ChatMemoryRepository repository =
            new TenantRoutingS3ChatMemoryRepository(s3Client, "bytechef-chat-memory", "");

        TenantContext.setCurrentTenantId("0000000001");

        try {
            repository.deleteByConversationId("conv-1");

            ArgumentCaptor<HeadBucketRequest> captor = ArgumentCaptor.forClass(HeadBucketRequest.class);

            verify(s3Client).headBucket(captor.capture());

            assertEquals("bytechef-chat-memory-0000000001", captor.getValue()
                .bucket());

            ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

            verify(s3Client).deleteObject(deleteCaptor.capture());

            assertEquals("bytechef-chat-memory-0000000001", deleteCaptor.getValue()
                .bucket());
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    @Test
    void testCreatesBucketWithLocationConstraintOutsideUsEast1() {
        CreateBucketRequest createBucketRequest = createBucketRequest(Region.EU_CENTRAL_1);

        CreateBucketConfiguration createBucketConfiguration = createBucketRequest.createBucketConfiguration();

        assertNotNull(createBucketConfiguration);
        assertEquals("eu-central-1", createBucketConfiguration.locationConstraintAsString());
    }

    @Test
    void testCreatesBucketWithoutLocationConstraintInUsEast1() {
        CreateBucketRequest createBucketRequest = createBucketRequest(Region.US_EAST_1);

        assertNull(createBucketRequest.createBucketConfiguration());
    }

    @Test
    void testInaccessibleExistingBucketNamesTheBucketPrefixProperty() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Exception(403, "Access Denied"));

        assertBucketFailureNamesTheBucketPrefixProperty("exists but is not accessible");
    }

    @Test
    void testForbiddenBucketCreationNamesTheBucketPrefixProperty() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder()
            .build());
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenThrow(s3Exception(403, "Access Denied"));

        assertBucketFailureNamesTheBucketPrefixProperty("cannot be created: access denied");
    }

    @Test
    void testBucketOwnedByAnotherAccountNamesTheBucketPrefixProperty() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder()
            .build());
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenThrow(BucketAlreadyExistsException.builder()
            .statusCode(409)
            .build());

        assertBucketFailureNamesTheBucketPrefixProperty("is owned by another AWS account");
    }

    @Test
    void testOtherBucketCheckFailuresAreRethrownUnchanged() {
        S3Exception serviceUnavailableException = s3Exception(503, "Service Unavailable");

        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(serviceUnavailableException);

        TenantRoutingS3ChatMemoryRepository repository =
            new TenantRoutingS3ChatMemoryRepository(s3Client, "bytechef-chat-memory", "");

        assertThatThrownBy(() -> deleteConversationAsTenant(repository))
            .isSameAs(serviceUnavailableException);
    }

    private void assertBucketFailureNamesTheBucketPrefixProperty(String expectedMessage) {
        TenantRoutingS3ChatMemoryRepository repository =
            new TenantRoutingS3ChatMemoryRepository(s3Client, "bytechef-chat-memory", "");

        assertThatThrownBy(() -> deleteConversationAsTenant(repository))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bytechef-chat-memory-0000000001")
            .hasMessageContaining(expectedMessage)
            .hasMessageContaining("bytechef.ai.memory.aws.bucket-prefix")
            .hasCauseInstanceOf(S3Exception.class);
    }

    private static void deleteConversationAsTenant(TenantRoutingS3ChatMemoryRepository repository) {
        TenantContext.setCurrentTenantId("0000000001");

        try {
            repository.deleteByConversationId("conv-1");
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    private static S3Exception s3Exception(int statusCode, String message) {
        return (S3Exception) S3Exception.builder()
            .statusCode(statusCode)
            .message(message)
            .build();
    }

    private CreateBucketRequest createBucketRequest(Region region) {
        when(s3Client.serviceClientConfiguration()).thenReturn(S3ServiceClientConfiguration.builder()
            .region(region)
            .build());
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder()
            .build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder()
            .build());

        TenantRoutingS3ChatMemoryRepository repository =
            new TenantRoutingS3ChatMemoryRepository(s3Client, "bytechef-chat-memory", "");

        TenantContext.runWithTenantId("0000000001", () -> repository.deleteByConversationId("conv-1"));

        ArgumentCaptor<CreateBucketRequest> createBucketRequestCaptor = ArgumentCaptor.forClass(
            CreateBucketRequest.class);

        verify(s3Client).createBucket(createBucketRequestCaptor.capture());

        return createBucketRequestCaptor.getValue();
    }
}
