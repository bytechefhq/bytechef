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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.TenantRoutingS3SessionRepository;
import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
public class BuiltInSessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of(@Nullable JdbcTemplate jdbcTemplate) {
        return of(jdbcTemplate, null, null, "");
    }

    public static ClusterElementDefinition<SessionRepositoryFunction> of(
        @Nullable JdbcTemplate jdbcTemplate, @Nullable S3Client s3Client, @Nullable String bucketPrefix,
        String keyPrefix) {

        SessionRepository sessionRepository = resolve(jdbcTemplate, s3Client, bucketPrefix, keyPrefix);

        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("Built-in Session Repository")
            .description("Stores session events using the application's configured session backend.")
            .type(SESSION_REPOSITORY)
            .object(
                () -> (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository);
    }

    private BuiltInSessionChatMemory() {
    }

    private static SessionRepository resolve(
        @Nullable JdbcTemplate jdbcTemplate, @Nullable S3Client s3Client, @Nullable String bucketPrefix,
        String keyPrefix) {

        if (s3Client != null && bucketPrefix != null) {
            return new TenantRoutingS3SessionRepository(s3Client, bucketPrefix, keyPrefix);
        }

        DataSource dataSource = jdbcTemplate == null ? null : jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return InMemorySessionRepository.builder()
                .build();
        }

        return SessionChatMemoryUtils.getSessionRepository(dataSource);
    }
}
