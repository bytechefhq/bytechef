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

import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/**
 * @author Ivica Cardic
 */
@Order(1200)
class AwsChatMemoryTenantTeardownContributor implements TenantTeardownContributor {

    private static final Logger log = LoggerFactory.getLogger(AwsChatMemoryTenantTeardownContributor.class);

    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final String bucketPrefix;

    AwsChatMemoryTenantTeardownContributor(S3Client s3Client, String bucketPrefix) {
        this.s3Client = s3Client;
        this.bucketPrefix = bucketPrefix;
    }

    @Override
    public void afterSchemaDrop(String tenantId) {
        String bucketName = bucketPrefix + "-" + TenantIdUtils.requireValid(tenantId);

        try {
            emptyBucket(bucketName);

            s3Client.deleteBucket(
                DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (NoSuchBucketException noSuchBucketException) {
            log.debug("Chat memory bucket {} does not exist", bucketName);
        }
    }

    private void emptyBucket(String bucketName) {
        String continuationToken = null;

        do {
            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(DELETE_BATCH_SIZE)
                    .continuationToken(continuationToken)
                    .build());

            List<ObjectIdentifier> objectIdentifiers = listObjectsV2Response.contents()
                .stream()
                .map(s3Object -> ObjectIdentifier.builder()
                    .key(s3Object.key())
                    .build())
                .toList();

            if (!objectIdentifiers.isEmpty()) {
                s3Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder()
                            .objects(objectIdentifiers)
                            .quiet(true)
                            .build())
                        .build());
            }

            continuationToken = Boolean.TRUE.equals(listObjectsV2Response.isTruncated())
                ? listObjectsV2Response.nextContinuationToken() : null;
        } while (continuationToken != null);
    }
}
