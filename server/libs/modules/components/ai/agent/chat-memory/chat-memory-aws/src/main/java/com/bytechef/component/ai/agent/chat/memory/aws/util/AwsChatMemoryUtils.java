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
            .s3Client(buildS3Client(connectionParameters))
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
            .build();
    }

    private static S3Client buildS3Client(Parameters connectionParameters) {
        return S3Client.builder()
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY))))
            .build();
    }
}
