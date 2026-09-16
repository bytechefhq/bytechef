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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author Ivica Cardic
 */
class TenantBucketS3SessionRepositoryTest {

    private static final String BUCKET_PREFIX = "bytechef-session";
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

        SessionRepository repository = createRepository(s3Client);

        assertThatThrownBy(() -> TenantContext.runWithTenantId("public", () -> repository.save(createSession())))
            .hasCauseInstanceOf(IllegalStateException.class)
            .rootCause()
            .isInstanceOf(BucketAlreadyExistsException.class);
    }

    @Test
    void testInaccessibleExistingBucketFailsWithGuidance() {
        S3Client s3Client = mock(S3Client.class);

        S3Exception forbiddenException = (S3Exception) S3Exception.builder()
            .statusCode(403)
            .build();

        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(forbiddenException);

        SessionRepository repository = createRepository(s3Client);

        assertThatThrownBy(() -> TenantContext.runWithTenantId("public", () -> repository.findById("session-1")))
            .cause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bytechef-session-public exists but is not accessible")
            .hasMessageContaining("bytechef.ai.memory.aws.session-bucket-prefix")
            .hasCause(forbiddenException);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void testForbiddenBucketCreationFailsWithGuidance() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        S3Exception forbiddenException = (S3Exception) S3Exception.builder()
            .statusCode(403)
            .build();

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
            .thenThrow(forbiddenException);

        SessionRepository repository = createRepository(s3Client);

        assertThatThrownBy(() -> TenantContext.runWithTenantId("public", () -> repository.save(createSession())))
            .cause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bytechef-session-public cannot be created")
            .hasMessageContaining("bytechef.ai.memory.aws.session-bucket-prefix")
            .hasCause(forbiddenException);
    }

    @Test
    void testBucketCreatedConcurrentlyByTheSameAccountIsUsed() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
            .thenThrow(BucketAlreadyOwnedByYouException.builder()
                .build());

        SessionRepository repository = createRepository(s3Client);

        TenantContext.runWithTenantId("public", () -> repository.save(createSession()));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUnexpectedHeadBucketFailureIsRethrown() {
        S3Client s3Client = mock(S3Client.class);

        S3Exception serverException = (S3Exception) S3Exception.builder()
            .statusCode(500)
            .build();

        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(serverException);

        SessionRepository repository = createRepository(s3Client);

        assertThatThrownBy(() -> TenantContext.runWithTenantId("public", () -> repository.findById("session-1")))
            .hasCause(serverException);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void testReadsAreRoutedToTheCurrentTenantBucket() {
        S3Client s3Client = mockS3ClientWithExistingBucket();

        SessionRepository repository = createRepository(s3Client);

        TenantContext.runWithTenantId("tenanta", () -> repository.findById("session-1"));
        TenantContext.runWithTenantId("tenantb", () -> repository.findById("session-1"));

        ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);

        verify(s3Client, times(2)).getObjectAsBytes(getObjectRequestCaptor.capture());

        assertThat(getObjectRequestCaptor.getAllValues())
            .extracting(GetObjectRequest::bucket)
            .containsExactly("bytechef-session-tenanta", "bytechef-session-tenantb");
    }

    @Test
    void testWritesAreRoutedToTheCurrentTenantBucket() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        SessionRepository repository = createRepository(s3Client);

        TenantContext.runWithTenantId("tenanta", () -> repository.save(createSession()));
        TenantContext.runWithTenantId("tenantb", () -> repository.save(createSession()));

        ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client, times(2)).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        assertThat(putObjectRequestCaptor.getAllValues())
            .extracting(PutObjectRequest::bucket)
            .containsExactly("bytechef-session-tenanta", "bytechef-session-tenantb");
    }

    @Test
    void testReadsOnAMissingBucketReturnNothingAndNeverCreateIt() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        SessionRepository repository = createRepository(s3Client);

        TenantContext.runWithTenantId("tenanta", () -> {
            assertThat(repository.findById("session-1")).isNull();
            assertThat(repository.findByUserId("user-1")).isEmpty();
            assertThat(repository.getEventVersion("session-1")).isZero();
            assertThat(repository.findEvents("session-1", EventFilter.all())).isEmpty();
            assertThat(repository.findEventsByUserId("user-1", EventFilter.all())).isEmpty();
            assertThat(repository.deleteExpiredSessions(Instant.now())).isZero();

            repository.delete("session-1");
        });

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, never()).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @Test
    void testDeletingExpiredSessionsNeverCreatesAMissingBucket() {
        S3Client s3Client = mockS3ClientWithMissingBucket(Region.US_EAST_1);

        SessionRepository repository = createRepository(s3Client);

        int deletedCount = TenantContext.callWithTenantId(
            "tenanta", () -> repository.deleteExpiredSessions(Instant.now()));

        assertThat(deletedCount).isZero();

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void testCleanupAfterRestartDeletesExpiredSessionsFromAnExistingBucket() {
        InMemoryS3Client inMemoryS3Client = new InMemoryS3Client();

        inMemoryS3Client.addBucket("bytechef-session-tenanta");

        Instant now = Instant.now();

        SessionRepository repositoryBeforeRestart = createRepository(inMemoryS3Client.getS3Client());

        TenantContext.runWithTenantId("tenanta", () -> repositoryBeforeRestart.save(Session.builder()
            .id("expired-session")
            .userId("user-1")
            .createdAt(now.minus(Duration.ofDays(2)))
            .expiresAt(now.minus(Duration.ofDays(1)))
            .build()));

        SessionRepository repositoryAfterRestart = createRepository(inMemoryS3Client.getS3Client());

        int deletedCount = TenantContext.callWithTenantId(
            "tenanta", () -> repositoryAfterRestart.deleteExpiredSessions(now));

        assertThat(deletedCount).isEqualTo(1);
        assertThat(inMemoryS3Client.getObjectKeys("bytechef-session-tenanta")).isEmpty();
    }

    @Test
    void testSaveIfAbsentInsertsOnlyOnce() {
        InMemoryS3Client inMemoryS3Client = new InMemoryS3Client();

        SessionRepository repository = createRepository(inMemoryS3Client.getS3Client());

        TenantContext.runWithTenantId("tenanta", () -> {
            assertThat(repository.saveIfAbsent(createSession())).isTrue();
            assertThat(repository.saveIfAbsent(createSession())).isFalse();
        });

        assertThat(inMemoryS3Client.getObjectKeys("bytechef-session-tenanta")).containsExactly("session-1.json");
    }

    @Test
    void testBlankBucketPrefixIsRejectedAtConstruction() {
        S3Client s3Client = mock(S3Client.class);

        assertThatThrownBy(() -> BuiltInSessionRepositoryFactory.createS3SessionRepository(s3Client, " ", "", 1000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.ai.memory.aws.session-bucket-prefix");
    }

    @Test
    void testNegativeMaxArchivedEventsIsRejectedAtConstruction() {
        S3Client s3Client = mock(S3Client.class);

        assertThatThrownBy(
            () -> BuiltInSessionRepositoryFactory.createS3SessionRepository(s3Client, BUCKET_PREFIX, "", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytechef.ai.memory.session-max-archived-events");
    }

    private static SessionRepository createRepository(S3Client s3Client) {
        return BuiltInSessionRepositoryFactory.createS3SessionRepository(s3Client, BUCKET_PREFIX, "", 1000);
    }

    private static Session createSession() {
        return Session.builder()
            .id("session-1")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();
    }

    private static String createBucketNameForTenant(String tenantId) {
        return createBucketRequest(tenantId, Region.US_EAST_1).bucket();
    }

    private static CreateBucketRequest createBucketRequest(String tenantId, Region region) {
        S3Client s3Client = mockS3ClientWithMissingBucket(region);

        SessionRepository repository = createRepository(s3Client);

        TenantContext.runWithTenantId(tenantId, () -> repository.save(createSession()));

        ArgumentCaptor<CreateBucketRequest> requestCaptor = ArgumentCaptor.forClass(CreateBucketRequest.class);

        verify(s3Client).createBucket(requestCaptor.capture());

        return requestCaptor.getValue();
    }

    private static S3Client mockS3ClientWithExistingBucket() {
        S3Client s3Client = mock(S3Client.class);

        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenReturn(HeadBucketResponse.builder()
                .build());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        return s3Client;
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
