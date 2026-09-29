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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Factory for creating {@link S3Client} instances from the AWS chat memory configuration.
 *
 * @author Ivica Cardic
 */
final class AwsS3ClientFactory {

    private AwsS3ClientFactory() {
    }

    static S3Client create(Aws memoryAws, Cloud.@Nullable Aws cloudAws) {
        S3ClientBuilder builder = S3Client.builder();

        Cloud.Aws fallbackAws = cloudAws == null ? new Cloud.Aws() : cloudAws;

        String region = StringUtils.firstNonBlank(memoryAws.getRegion(), fallbackAws.getRegion());

        if (region != null) {
            builder.region(Region.of(region));
        }

        return builder.credentialsProvider(getAwsCredentialsProvider(memoryAws, fallbackAws))
            .build();
    }

    static AwsCredentialsProvider getAwsCredentialsProvider(Aws memoryAws, Cloud.Aws cloudAws) {
        AwsBasicCredentials memoryAwsCredentials = getAwsCredentials(
            memoryAws.getAccessKeyId(), memoryAws.getSecretAccessKey(), "bytechef.ai.memory.aws.");

        if (memoryAwsCredentials != null) {
            return StaticCredentialsProvider.create(memoryAwsCredentials);
        }

        AwsBasicCredentials cloudAwsCredentials = getAwsCredentials(
            cloudAws.getAccessKeyId(), cloudAws.getSecretAccessKey(), "bytechef.cloud.aws.");

        if (cloudAwsCredentials != null) {
            return StaticCredentialsProvider.create(cloudAwsCredentials);
        }

        return DefaultCredentialsProvider.builder()
            .build();
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
