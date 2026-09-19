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

package com.bytechef.component.ai.agent.chat.memory.aws.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.s3.S3ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public class AwsChatMemoryUtils {

    private static final Duration CLIENT_IDLE_TIMEOUT = Duration.ofHours(24);

    private static final Cache<S3ClientKey, S3Client> S3_CLIENTS = Caffeine.newBuilder()
        .expireAfterAccess(CLIENT_IDLE_TIMEOUT)
        .removalListener((S3ClientKey s3ClientKey, S3Client s3Client, RemovalCause removalCause) -> {
            if (s3Client != null) {
                s3Client.close();
            }
        })
        .build();

    private AwsChatMemoryUtils() {
    }

    public static ActionDefinition.OptionsFunction<String> getFirstMessages() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            try (S3Client s3Client = buildS3Client(connectionParameters)) {
                ChatMemoryRepository chatMemoryRepository = S3ChatMemoryRepository.builder()
                    .s3Client(s3Client)
                    .bucketName(connectionParameters.getRequiredString(BUCKET))
                    .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
                    .build();

                List<String> conversationIds = chatMemoryRepository.findConversationIds();

                for (String conversationId : conversationIds) {
                    List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

                    if (messages.isEmpty()) {
                        options.add(option(conversationId, conversationId));
                    } else {
                        Message message = messages.getFirst();

                        options.add(option(conversationId, conversationId, message.getText()));
                    }
                }
            }

            return options;
        };
    }

    public static S3ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        return S3ChatMemoryRepository.builder()
            .s3Client(getSharedS3Client(connectionParameters))
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
            .build();
    }

    static S3Client getSharedS3Client(Parameters connectionParameters) {
        return S3_CLIENTS.get(toS3ClientKey(connectionParameters), AwsChatMemoryUtils::buildS3Client);
    }

    private static S3Client buildS3Client(Parameters connectionParameters) {
        return buildS3Client(toS3ClientKey(connectionParameters));
    }

    private static S3Client buildS3Client(S3ClientKey s3ClientKey) {
        return S3Client.builder()
            .region(Region.of(s3ClientKey.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3ClientKey.accessKeyId(), s3ClientKey.secretAccessKey())))
            .build();
    }

    private static S3ClientKey toS3ClientKey(Parameters connectionParameters) {
        return new S3ClientKey(
            connectionParameters.getRequiredString(REGION), connectionParameters.getRequiredString(ACCESS_KEY_ID),
            connectionParameters.getRequiredString(SECRET_ACCESS_KEY));
    }

    private record S3ClientKey(String region, String accessKeyId, String secretAccessKey) {

        @Override
        public String toString() {
            return "S3ClientKey{region=" + region + ", accessKeyId=" + accessKeyId + "}";
        }
    }
}
