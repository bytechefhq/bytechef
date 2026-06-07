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

package com.bytechef.component.ai.agent.chat.memory.aws.session.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.SECRET_ACCESS_KEY;

import com.bytechef.component.definition.Parameters;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.s3.S3SessionRepository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public class AwsSessionChatMemoryUtils {

    private AwsSessionChatMemoryUtils() {
    }

    public static SessionRepository getSessionRepository(Parameters connectionParameters) {
        S3Client s3Client = S3Client.builder()
            .region(Region.of(connectionParameters.getRequiredString(REGION)))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY))))
            .build();

        return S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, ""))
            .build();
    }
}
