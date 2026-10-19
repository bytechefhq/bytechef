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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author Ivica Cardic
 */
class AwsChatMemoryTenantTeardownContributorTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final AwsChatMemoryTenantTeardownContributor contributor =
        new AwsChatMemoryTenantTeardownContributor(s3Client, "chat");

    @Test
    void testEmptiesAndDeletesTheTenantBucket() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
            ListObjectsV2Response.builder()
                .contents(S3Object.builder()
                    .key("k1")
                    .build())
                .isTruncated(false)
                .build());

        contributor.afterSchemaDrop("000001");

        ArgumentCaptor<ListObjectsV2Request> listCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);

        verify(s3Client).listObjectsV2(listCaptor.capture());

        assertThat(listCaptor.getValue()
            .bucket()).isEqualTo("chat-000001");

        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

        ArgumentCaptor<DeleteBucketRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteBucketRequest.class);

        verify(s3Client).deleteBucket(deleteCaptor.capture());

        assertThat(deleteCaptor.getValue()
            .bucket()).isEqualTo("chat-000001");
    }

    @Test
    void testMissingBucketIsNoOp() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenThrow(NoSuchBucketException.builder()
                .build());

        contributor.afterSchemaDrop("000001");

        verify(s3Client, never()).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("a/b"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(s3Client);
    }
}
