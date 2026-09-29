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

import com.bytechef.config.ApplicationProperties.Ai.Memory.Aws;
import com.bytechef.config.ApplicationProperties.Cloud;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Marko Kriskovic
 */
class AwsS3ClientFactoryTest {

    @Test
    void testRegionFallsBackToCloudAws() {
        Cloud.Aws cloudAws = new Cloud.Aws();

        cloudAws.setRegion("eu-north-1");

        try (S3Client s3Client = AwsS3ClientFactory.create(new Aws(), cloudAws)) {
            assertThat(s3Client.serviceClientConfiguration()
                .region()).isEqualTo(Region.EU_NORTH_1);
        }
    }

    @Test
    void testMemoryRegionTakesPrecedenceOverCloudAws() {
        Aws memoryAws = new Aws();

        memoryAws.setRegion("eu-west-1");

        Cloud.Aws cloudAws = new Cloud.Aws();

        cloudAws.setRegion("eu-north-1");

        try (S3Client s3Client = AwsS3ClientFactory.create(memoryAws, cloudAws)) {
            assertThat(s3Client.serviceClientConfiguration()
                .region()).isEqualTo(Region.EU_WEST_1);
        }
    }

    @Test
    void testMemoryCredentialsTakePrecedenceOverCloudCredentials() {
        AwsCredentials awsCredentials = AwsS3ClientFactory.getAwsCredentialsProvider(
            memoryAws("memory-access-key", "memory-secret-key"), cloudAws("cloud-access-key", "cloud-secret-key"))
            .resolveCredentials();

        assertThat(awsCredentials.accessKeyId()).isEqualTo("memory-access-key");
        assertThat(awsCredentials.secretAccessKey()).isEqualTo("memory-secret-key");
    }

    @Test
    void testCloudCredentialsAreUsedWhenMemoryCredentialsAreNotSet() {
        AwsCredentials awsCredentials = AwsS3ClientFactory.getAwsCredentialsProvider(
            new Aws(), cloudAws("cloud-access-key", "cloud-secret-key"))
            .resolveCredentials();

        assertThat(awsCredentials.accessKeyId()).isEqualTo("cloud-access-key");
        assertThat(awsCredentials.secretAccessKey()).isEqualTo("cloud-secret-key");
    }

    @Test
    void testHalfConfiguredMemoryCredentialsAreRejected() {
        Aws memoryAws = memoryAws("memory-access-key", " ");
        Cloud.Aws cloudAws = cloudAws("cloud-access-key", "cloud-secret-key");

        assertThatThrownBy(() -> AwsS3ClientFactory.getAwsCredentialsProvider(memoryAws, cloudAws))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.ai.memory.aws.secret-access-key");
    }

    @Test
    void testDefaultCredentialsProviderIsUsedWhenNoCredentialsAreSet() {
        assertThat(AwsS3ClientFactory.getAwsCredentialsProvider(new Aws(), new Cloud.Aws()))
            .isInstanceOf(DefaultCredentialsProvider.class);
    }

    private static Cloud.Aws cloudAws(String accessKeyId, String secretAccessKey) {
        Cloud.Aws cloudAws = new Cloud.Aws();

        cloudAws.setAccessKeyId(accessKeyId);
        cloudAws.setSecretAccessKey(secretAccessKey);

        return cloudAws;
    }

    private static Aws memoryAws(String accessKeyId, String secretAccessKey) {
        Aws memoryAws = new Aws();

        memoryAws.setAccessKeyId(accessKeyId);
        memoryAws.setSecretAccessKey(secretAccessKey);

        return memoryAws;
    }
}
