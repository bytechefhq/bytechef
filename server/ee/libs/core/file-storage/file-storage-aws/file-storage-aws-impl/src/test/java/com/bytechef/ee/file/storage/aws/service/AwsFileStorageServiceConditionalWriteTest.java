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

package com.bytechef.ee.file.storage.aws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.VersionedFileContent;
import com.bytechef.file.storage.exception.FileStorageException;
import io.awspring.cloud.s3.S3Template;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @version ee
 *
 * @author Asish Kumar
 */
class AwsFileStorageServiceConditionalWriteTest {

    private static final String DIRECTORY = "directory";
    private static final String ETAG = "\"etag-1\"";
    private static final String FILENAME = "file";

    private S3Client s3Client;
    private AwsFileStorageServiceImpl storageService;

    @BeforeEach
    void beforeEach() {
        s3Client = mock(S3Client.class);

        storageService = new AwsFileStorageServiceImpl(s3Client, mock(S3Template.class), "bucket");
    }

    @Test
    void testReadFileContentVersionedReturnsContentAndETag() {
        byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(
                ResponseBytes.fromByteArray(
                    GetObjectResponse.builder()
                        .eTag(ETAG)
                        .build(),
                    Base64.getEncoder()
                        .encode(content)));

        Optional<VersionedFileContent> versionedFileContentOptional = storageService.readFileContentVersioned(
            DIRECTORY, FILENAME);

        assertThat(versionedFileContentOptional).isPresent();

        VersionedFileContent versionedFileContent = versionedFileContentOptional.get();

        assertThat(versionedFileContent.content()).isEqualTo(content);
        assertThat(versionedFileContent.version()).isEqualTo(ETAG);
    }

    @Test
    void testReadFileContentVersionedReturnsEmptyWhenTheFileDoesNotExist() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        assertThat(storageService.readFileContentVersioned(DIRECTORY, FILENAME)).isEmpty();
    }

    @Test
    void testStoreFileContentIfVersionRequiresTheFileToNotExistWithoutExpectedVersion() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        assertThat(storageService.storeFileContentIfVersion(DIRECTORY, FILENAME, new byte[] {
            1
        }, null)).isTrue();

        ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        assertThat(putObjectRequestCaptor.getValue()
            .ifNoneMatch()).isEqualTo("*");
        assertThat(putObjectRequestCaptor.getValue()
            .ifMatch()).isNull();
    }

    @Test
    void testStoreFileContentIfVersionRequiresTheExpectedETag() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        assertThat(storageService.storeFileContentIfVersion(DIRECTORY, FILENAME, new byte[] {
            1
        }, ETAG)).isTrue();

        ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        assertThat(putObjectRequestCaptor.getValue()
            .ifMatch()).isEqualTo(ETAG);
        assertThat(putObjectRequestCaptor.getValue()
            .ifNoneMatch()).isNull();
    }

    @Test
    void testStoreFileContentIfVersionReturnsFalseWhenTheConditionIsNotMet() {
        for (int statusCode : new int[] {
            409, 412
        }) {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder()
                    .statusCode(statusCode)
                    .build());

            assertThat(storageService.storeFileContentIfVersion(DIRECTORY, FILENAME, new byte[] {
                1
            }, ETAG)).isFalse();
        }

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(NoSuchKeyException.builder()
                .statusCode(404)
                .build());

        assertThat(storageService.storeFileContentIfVersion(DIRECTORY, FILENAME, new byte[] {
            1
        }, ETAG)).isFalse();
    }

    @Test
    void testStoreFileContentIfVersionFailsOnOtherErrors() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(500)
                .build());

        assertThatThrownBy(() -> storageService.storeFileContentIfVersion(DIRECTORY, FILENAME, new byte[] {
            1
        }, ETAG)).isInstanceOf(FileStorageException.class);
    }
}
