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

package com.bytechef.component.ai.agent.chat.memory.builtin.session;

import static com.bytechef.component.ai.agent.chat.memory.builtin.session.constant.BuiltInSessionChatMemoryConstants.BUILT_IN_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster.BuiltInSessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * @author Ivica Cardic
 */
@Component(BUILT_IN_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class BuiltInSessionChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;
    private final S3Client s3Client;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public BuiltInSessionChatMemoryComponentHandler(
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate, Environment environment) {

        boolean awsProvider = "aws".equals(environment.getProperty("bytechef.ai.session.provider"));

        String bucketPrefix;
        String keyPrefix;

        if (awsProvider) {
            s3Client = buildS3Client(environment);
            bucketPrefix = environment.getProperty("bytechef.ai.session.aws.bucket-prefix", "bytechef-session");
            keyPrefix = environment.getProperty("bytechef.ai.session.aws.key-prefix", "");
        } else {
            s3Client = null;
            bucketPrefix = null;
            keyPrefix = "";
        }

        this.componentDefinition = component(BUILT_IN_SESSION_CHAT_MEMORY)
            .title("Built-in Session Repository")
            .description("Built-in storage backend for Session Chat Memory.")
            .icon("path:assets/built-in-session-chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(BuiltInSessionChatMemory.of(jdbcTemplate, s3Client, bucketPrefix, keyPrefix));
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static S3Client buildS3Client(Environment environment) {
        S3ClientBuilder builder = S3Client.builder();

        String region = environment.getProperty("bytechef.ai.session.aws.region");

        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }

        String accessKeyId = environment.getProperty("bytechef.ai.session.aws.access-key-id");
        String secretAccessKey = environment.getProperty("bytechef.ai.session.aws.secret-access-key");

        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
