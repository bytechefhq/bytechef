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

import com.bytechef.ai.chat.memory.aws.config.AwsS3ClientFactory;
import com.bytechef.config.ApplicationProperties.Ai.Memory;
import com.bytechef.config.ApplicationProperties.Cloud;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * @author Ivica Cardic
 */
class AwsCredentialsResolutionParityTest {

    @ParameterizedTest
    @MethodSource("credentialCombinations")
    void testSessionAndChatMemoryResolveCredentialsIdentically(
        @Nullable String memoryAccessKeyId, @Nullable String memorySecretAccessKey,
        @Nullable String cloudAccessKeyId, @Nullable String cloudSecretAccessKey) {

        MockEnvironment environment = new MockEnvironment();

        setIfPresent(environment, "bytechef.ai.memory.aws.access-key-id", memoryAccessKeyId);
        setIfPresent(environment, "bytechef.ai.memory.aws.secret-access-key", memorySecretAccessKey);
        setIfPresent(environment, "bytechef.cloud.aws.access-key-id", cloudAccessKeyId);
        setIfPresent(environment, "bytechef.cloud.aws.secret-access-key", cloudSecretAccessKey);

        Memory.Aws memoryAws = new Memory.Aws();

        memoryAws.setAccessKeyId(memoryAccessKeyId);
        memoryAws.setSecretAccessKey(memorySecretAccessKey);

        Cloud.Aws cloudAws = new Cloud.Aws();

        cloudAws.setAccessKeyId(cloudAccessKeyId);
        cloudAws.setSecretAccessKey(cloudSecretAccessKey);

        String sessionOutcome = describe(() -> BuiltInSessionRepositoryFactory.getAwsCredentialsProvider(environment));
        String chatMemoryOutcome = describe(() -> AwsS3ClientFactory.getAwsCredentialsProvider(memoryAws, cloudAws));

        assertThat(sessionOutcome).isEqualTo(chatMemoryOutcome);
    }

    private static Stream<Arguments> credentialCombinations() {
        return Stream.of(
            Arguments.of(null, null, null, null),
            Arguments.of("memory-access-key", "memory-secret-key", null, null),
            Arguments.of(null, null, "cloud-access-key", "cloud-secret-key"),
            Arguments.of("memory-access-key", "memory-secret-key", "cloud-access-key", "cloud-secret-key"),
            Arguments.of("memory-access-key", " ", "cloud-access-key", "cloud-secret-key"),
            Arguments.of(null, "memory-secret-key", null, null),
            Arguments.of(null, null, "cloud-access-key", null),
            Arguments.of(" ", " ", "cloud-access-key", "cloud-secret-key"));
    }

    private static String describe(Supplier<AwsCredentialsProvider> awsCredentialsProviderSupplier) {
        AwsCredentialsProvider awsCredentialsProvider;

        try {
            awsCredentialsProvider = awsCredentialsProviderSupplier.get();
        } catch (IllegalArgumentException illegalArgumentException) {
            return "rejected: " + illegalArgumentException.getMessage();
        }

        if (awsCredentialsProvider instanceof StaticCredentialsProvider staticCredentialsProvider) {
            AwsCredentials awsCredentials = staticCredentialsProvider.resolveCredentials();

            return "static: " + awsCredentials.accessKeyId() + "/" + awsCredentials.secretAccessKey();
        }

        Class<? extends AwsCredentialsProvider> awsCredentialsProviderClass = awsCredentialsProvider.getClass();

        return "provider: " + awsCredentialsProviderClass.getName();
    }

    private static void setIfPresent(MockEnvironment environment, String propertyName, @Nullable String value) {
        if (value != null) {
            environment.setProperty(propertyName, value);
        }
    }
}
