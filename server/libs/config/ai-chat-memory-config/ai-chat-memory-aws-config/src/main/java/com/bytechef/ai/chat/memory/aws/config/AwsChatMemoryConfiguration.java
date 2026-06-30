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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Memory.Aws;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
@Configuration
class AwsChatMemoryConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "aws")
    ChatMemoryRepository awsChatMemoryRepository(ApplicationProperties applicationProperties) {
        Aws aws = applicationProperties.getAi()
            .getMemory()
            .getAws();

        S3Client s3Client = AwsS3ClientFactory.create(aws);

        return new TenantRoutingS3ChatMemoryRepository(s3Client, aws.getBucketPrefix(), aws.getKeyPrefix());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "aws")
    ChatMemory awsChatMemory(ChatMemoryRepository awsChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(awsChatMemoryRepository)
            .maxMessages(500)
            .build();
    }
}
