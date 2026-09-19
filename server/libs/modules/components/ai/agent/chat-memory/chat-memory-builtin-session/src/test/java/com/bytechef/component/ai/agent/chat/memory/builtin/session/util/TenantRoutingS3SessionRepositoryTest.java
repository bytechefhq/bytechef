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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * @author Ivica Cardic
 */
class TenantRoutingS3SessionRepositoryTest {

    private static final Pattern S3_BUCKET_NAME = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    @Test
    void testCreatesValidBucketForTenantIdWithUnderscore() {
        String bucketName = createBucketNameForTenant("tenant_one");

        assertThat(bucketName).matches(S3_BUCKET_NAME);
    }

    @Test
    void testCreatesValidBucketForLongTenantId() {
        String bucketName = createBucketNameForTenant("t".repeat(80));

        assertThat(bucketName).matches(S3_BUCKET_NAME);
    }

    @Test
    void testKeepsAlreadyValidBucketNameUnchanged() {
        assertThat(createBucketNameForTenant("public")).isEqualTo("bytechef-session-public");
    }

    @Test
    void testDistinctTenantIdsNeverShareABucket() {
        assertThat(createBucketNameForTenant("tenant_one")).isNotEqualTo(createBucketNameForTenant("tenant-one"));
    }

    @Test
    void testCreatesBucketWithLocationConstraintOutsideUsEast1() {
        CreateBucketConfiguration createBucketConfiguration = createBucketRequest("public", Region.EU_WEST_1)
            .createBucketConfiguration();

        assertThat(createBucketConfiguration).isNotNull();
        assertThat(createBucketConfiguration.locationConstraintAsString()).isEqualTo("eu-west-1");
    }

    @Test
    void testCreatesBucketWithoutLocationConstraintInUsEast1() {
        assertThat(createBucketRequest("public", Region.US_EAST_1).createBucketConfiguration()).isNull();
    }

    @Test
    void testBucketNameOwnedByAnotherAccountFailsLoudly() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
            .thenThrow(BucketAlreadyExistsException.builder()
                .build());

        TenantRoutingS3SessionRepository repository = new TenantRoutingS3SessionRepository(
            s3Client, "bytechef-session", "");

        assertThatThrownBy(() -> TenantContext.runWithTenantId("public", () -> repository.findById("session-1")))
            .hasCauseInstanceOf(IllegalStateException.class)
            .rootCause()
            .isInstanceOf(BucketAlreadyExistsException.class);
    }

    private static String createBucketNameForTenant(String tenantId) {
        return createBucketRequest(tenantId, Region.US_EAST_1).bucket();
    }

    private static CreateBucketRequest createBucketRequest(String tenantId, Region region) {
        S3Client s3Client = mockS3ClientWithMissingBucket(region);

        TenantRoutingS3SessionRepository repository = new TenantRoutingS3SessionRepository(
            s3Client, "bytechef-session", "");

        TenantContext.runWithTenantId(tenantId, () -> repository.findById("session-1"));

        ArgumentCaptor<CreateBucketRequest> requestCaptor = ArgumentCaptor.forClass(CreateBucketRequest.class);

        verify(s3Client).createBucket(requestCaptor.capture());

        return requestCaptor.getValue();
    }

    private static S3Client mockS3ClientWithMissingBucket(Region region) {
        S3Client s3Client = mock(S3Client.class);

        when(s3Client.serviceClientConfiguration())
            .thenReturn(S3ServiceClientConfiguration.builder()
                .region(region)
                .build());
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(NoSuchBucketException.builder()
                .build());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        return s3Client;
    }
}
