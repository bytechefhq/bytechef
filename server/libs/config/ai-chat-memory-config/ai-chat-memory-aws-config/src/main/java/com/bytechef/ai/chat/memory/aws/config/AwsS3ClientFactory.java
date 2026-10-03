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

import com.bytechef.config.ApplicationProperties.Ai.Memory.Aws;
import com.bytechef.config.ApplicationProperties.Cloud;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Creates {@link S3Client} instances for chat memory, resolving the region and credentials from
 * {@code bytechef.ai.memory.aws.*}, then {@code bytechef.cloud.aws.*}, then the default AWS provider chains.
 *
 * @author Ivica Cardic
 */
public final class AwsS3ClientFactory {

    private static final String CLOUD_AWS_PREFIX = "bytechef.cloud.aws.";
    private static final String MEMORY_AWS_PREFIX = "bytechef.ai.memory.aws.";

    private static final Logger log = LoggerFactory.getLogger(AwsS3ClientFactory.class);

    private AwsS3ClientFactory() {
    }

    private record ResolvedAwsCredentials(AwsCredentialsProvider awsCredentialsProvider, String source) {
    }

    static S3Client create(Aws memoryAws, Cloud.@Nullable Aws cloudAws) {
        S3ClientBuilder builder = S3Client.builder();

        Cloud.Aws fallbackAws = cloudAws == null ? new Cloud.Aws() : cloudAws;

        String region = StringUtils.firstNonBlank(memoryAws.getRegion(), fallbackAws.getRegion());

        if (region != null) {
            builder.region(Region.of(region));
        }

        ResolvedAwsCredentials resolvedAwsCredentials = resolveAwsCredentials(memoryAws, fallbackAws);

        log.info(
            "Chat memory S3 client uses AWS credentials from {} in region {}", resolvedAwsCredentials.source(),
            region == null ? "resolved by the default region provider chain" : region);

        return builder.credentialsProvider(resolvedAwsCredentials.awsCredentialsProvider())
            .build();
    }

    public static AwsCredentialsProvider getAwsCredentialsProvider(Aws memoryAws, Cloud.Aws cloudAws) {
        ResolvedAwsCredentials resolvedAwsCredentials = resolveAwsCredentials(memoryAws, cloudAws);

        return resolvedAwsCredentials.awsCredentialsProvider();
    }

    private static ResolvedAwsCredentials resolveAwsCredentials(Aws memoryAws, Cloud.Aws cloudAws) {
        AwsBasicCredentials memoryAwsCredentials = getAwsCredentials(
            memoryAws.getAccessKeyId(), memoryAws.getSecretAccessKey(), MEMORY_AWS_PREFIX);

        if (memoryAwsCredentials != null) {
            return new ResolvedAwsCredentials(
                StaticCredentialsProvider.create(memoryAwsCredentials), MEMORY_AWS_PREFIX + "*");
        }

        AwsBasicCredentials cloudAwsCredentials = getAwsCredentials(
            cloudAws.getAccessKeyId(), cloudAws.getSecretAccessKey(), CLOUD_AWS_PREFIX);

        if (cloudAwsCredentials != null) {
            return new ResolvedAwsCredentials(
                StaticCredentialsProvider.create(cloudAwsCredentials), CLOUD_AWS_PREFIX + "*");
        }

        return new ResolvedAwsCredentials(
            DefaultCredentialsProvider.builder()
                .build(),
            "the default credentials provider chain");
    }

    private static @Nullable AwsBasicCredentials getAwsCredentials(
        @Nullable String accessKeyId, @Nullable String secretAccessKey, String propertyPrefix) {

        if (StringUtils.isAllBlank(accessKeyId, secretAccessKey)) {
            return null;
        }

        if (StringUtils.isAnyBlank(accessKeyId, secretAccessKey)) {
            throw new IllegalArgumentException(
                propertyPrefix + "access-key-id and " + propertyPrefix + "secret-access-key must be set together");
        }

        return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    }
}
