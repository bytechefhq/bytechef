/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Template;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsFileStorageServiceDeleteTenantFilesTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final AwsFileStorageServiceImpl service = new AwsFileStorageServiceImpl(
        s3Client, mock(S3Template.class), "bucket");

    @Test
    void testDeletesEveryPageOfTheTenantPrefix() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(page(true, "t1", "000001/a", "000001/b"))
            .thenReturn(page(false, null, "000001/c"));

        service.deleteTenantFiles("000001");

        ArgumentCaptor<ListObjectsV2Request> listCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);

        verify(s3Client, times(2)).listObjectsV2(listCaptor.capture());

        assertThat(listCaptor.getAllValues()).extracting(ListObjectsV2Request::prefix)
            .containsOnly("000001/");
        assertThat(listCaptor.getAllValues()).extracting(ListObjectsV2Request::continuationToken)
            .containsExactly(null, "t1");

        ArgumentCaptor<DeleteObjectsRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);

        verify(s3Client, times(2)).deleteObjects(deleteCaptor.capture());

        assertThat(deleteCaptor.getAllValues()
            .get(0)
            .delete()
            .objects()).hasSize(2);
        assertThat(deleteCaptor.getAllValues()
            .get(1)
            .delete()
            .objects()).hasSize(1);
    }

    @Test
    void testEmptyPrefixIsNoOp() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(page(false, null));

        service.deleteTenantFiles("000001");

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> service.deleteTenantFiles("0/../1"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(s3Client);
    }

    private static ListObjectsV2Response page(boolean truncated, String nextToken, String... keys) {
        List<S3Object> objects = java.util.Arrays.stream(keys)
            .map(key -> S3Object.builder()
                .key(key)
                .build())
            .toList();

        return ListObjectsV2Response.builder()
            .contents(objects)
            .isTruncated(truncated)
            .nextContinuationToken(nextToken)
            .build();
    }
}
