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

package com.bytechef.component.ai.agent.chat.memory.cassandra.util;

import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.CONTACT_POINTS;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.DATACENTER;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.KEYSPACE;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.TABLE;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import java.net.InetSocketAddress;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepositoryConfig;

/**
 * @author Ivica Cardic
 */
public class CassandraChatMemoryUtils {

    private CassandraChatMemoryUtils() {
    }

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        String contactPoints = connectionParameters.getRequiredString(CONTACT_POINTS);
        int port = connectionParameters.getRequiredInteger(PORT);
        String datacenter = connectionParameters.getRequiredString(DATACENTER);
        String keyspace = connectionParameters.getString(KEYSPACE);
        String table = connectionParameters.getString(TABLE);
        String username = connectionParameters.getString(USERNAME);
        String password = connectionParameters.getString(PASSWORD);

        CqlSessionBuilder sessionBuilder = CqlSession.builder()
            .withLocalDatacenter(datacenter);

        for (String contactPoint : contactPoints.split(",")) {
            String host = contactPoint.trim();

            if (!host.isEmpty()) {
                sessionBuilder.addContactPoint(new InetSocketAddress(host, port));
            }
        }

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            sessionBuilder.withAuthCredentials(username, password);
        }

        if (keyspace != null && !keyspace.isBlank()) {
            sessionBuilder.withKeyspace(keyspace);
        }

        CqlSession session = sessionBuilder.build();

        CassandraChatMemoryRepositoryConfig.Builder configBuilder =
            CassandraChatMemoryRepositoryConfig.builder()
                .withCqlSession(session);

        if (keyspace != null && !keyspace.isBlank()) {
            configBuilder.withKeyspaceName(keyspace);
        }

        if (table != null && !table.isBlank()) {
            configBuilder.withTableName(table);
        }

        CassandraChatMemoryRepositoryConfig config = configBuilder.build();

        return CassandraChatMemoryRepository.create(config);
    }
}
