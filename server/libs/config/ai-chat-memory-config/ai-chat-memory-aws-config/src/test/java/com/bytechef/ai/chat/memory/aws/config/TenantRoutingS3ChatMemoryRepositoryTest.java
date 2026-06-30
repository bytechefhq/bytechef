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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;

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
}
