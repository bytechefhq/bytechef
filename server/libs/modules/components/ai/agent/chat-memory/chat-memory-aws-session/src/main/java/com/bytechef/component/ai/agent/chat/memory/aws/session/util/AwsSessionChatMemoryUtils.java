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
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.DEFAULT_KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.session.constant.AwsSessionChatMemoryConstants.SECRET_ACCESS_KEY;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.commons.util.ClientCacheUtils;
import com.bytechef.component.definition.Parameters;
import com.github.benmanes.caffeine.cache.Cache;
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

    private static final Cache<S3ClientKey, S3Client> S3_CLIENTS = createClientCache(
        ClientCacheSettings.defaults());

    private AwsSessionChatMemoryUtils() {
    }

    static <K> Cache<K, S3Client> createClientCache(ClientCacheSettings clientCacheSettings) {
        return ClientCacheUtils.createClientCache(clientCacheSettings, S3Client::close);
    }

    public static SessionRepository getSessionRepository(Parameters connectionParameters) {
        return createSessionRepository(getSharedS3Client(connectionParameters), connectionParameters);
    }

    static SessionRepository createSessionRepository(S3Client s3Client, Parameters connectionParameters) {
        return S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(connectionParameters.getRequiredString(BUCKET))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, DEFAULT_KEY_PREFIX))
            .build();
    }

    static S3Client getSharedS3Client(Parameters connectionParameters) {
        S3ClientKey s3ClientKey = new S3ClientKey(
            connectionParameters.getRequiredString(REGION), connectionParameters.getRequiredString(ACCESS_KEY_ID),
            connectionParameters.getRequiredString(SECRET_ACCESS_KEY));

        return S3_CLIENTS.get(s3ClientKey, AwsSessionChatMemoryUtils::buildS3Client);
    }

    private static S3Client buildS3Client(S3ClientKey s3ClientKey) {
        return S3Client.builder()
            .region(Region.of(s3ClientKey.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3ClientKey.accessKeyId(), s3ClientKey.secretAccessKey())))
            .build();
    }

    private record S3ClientKey(String region, String accessKeyId, String secretAccessKey) {

        @Override
        public String toString() {
            return "S3ClientKey{region=" + region + ", accessKeyId=" + accessKeyId + "}";
        }
    }
}
