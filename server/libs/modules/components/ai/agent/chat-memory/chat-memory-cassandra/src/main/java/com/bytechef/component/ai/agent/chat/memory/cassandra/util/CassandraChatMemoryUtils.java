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
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepositoryConfig;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Ivica Cardic
 */
public class CassandraChatMemoryUtils {

    private static final Duration CLIENT_IDLE_TIMEOUT = Duration.ofHours(24);

    private static final Cache<CqlSessionKey, CqlSession> CLIENTS = Caffeine.newBuilder()
        .expireAfterAccess(CLIENT_IDLE_TIMEOUT)
        .removalListener((CqlSessionKey cqlSessionKey, CqlSession cqlSession, RemovalCause removalCause) -> {
            if (cqlSession != null) {
                cqlSession.close();
            }
        })
        .build();

    private CassandraChatMemoryUtils() {
    }

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        String keyspace = connectionParameters.getString(KEYSPACE);
        String table = connectionParameters.getString(TABLE);

        CassandraChatMemoryRepositoryConfig.Builder configBuilder = CassandraChatMemoryRepositoryConfig.builder()
            .withCqlSession(getSharedCqlSession(connectionParameters));

        if (keyspace != null && !keyspace.isBlank()) {
            configBuilder.withKeyspaceName(keyspace);
        }

        if (table != null && !table.isBlank()) {
            configBuilder.withTableName(table);
        }

        CassandraChatMemoryRepositoryConfig config = configBuilder.build();

        return CassandraChatMemoryRepository.create(config);
    }

    static CqlSession getSharedCqlSession(Parameters connectionParameters) {
        return CLIENTS.get(toCqlSessionKey(connectionParameters), CassandraChatMemoryUtils::buildCqlSession);
    }

    private static CqlSession buildCqlSession(CqlSessionKey cqlSessionKey) {
        CqlSessionBuilder sessionBuilder = CqlSession.builder()
            .withLocalDatacenter(cqlSessionKey.datacenter());

        for (String contactPoint : cqlSessionKey.contactPoints()
            .split(",")) {

            String host = contactPoint.trim();

            if (!host.isEmpty()) {
                sessionBuilder.addContactPoint(new InetSocketAddress(host, cqlSessionKey.port()));
            }
        }

        String username = cqlSessionKey.username();
        String password = cqlSessionKey.password();

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            sessionBuilder.withAuthCredentials(username, password);
        }

        String keyspace = cqlSessionKey.keyspace();

        if (keyspace != null && !keyspace.isBlank()) {
            sessionBuilder.withKeyspace(keyspace);
        }

        return sessionBuilder.build();
    }

    private static CqlSessionKey toCqlSessionKey(Parameters connectionParameters) {
        return new CqlSessionKey(
            connectionParameters.getRequiredString(CONTACT_POINTS), connectionParameters.getRequiredInteger(PORT),
            connectionParameters.getRequiredString(DATACENTER), connectionParameters.getString(KEYSPACE),
            connectionParameters.getString(USERNAME), connectionParameters.getString(PASSWORD));
    }

    private record CqlSessionKey(
        String contactPoints, int port, String datacenter, @Nullable String keyspace, @Nullable String username,
        @Nullable String password) {

        @Override
        public String toString() {
            return "CqlSessionKey{contactPoints=" + contactPoints + ", port=" + port + ", datacenter=" + datacenter +
                ", keyspace=" + keyspace + ", username=" + username + "}";
        }
    }

    public static ActionDefinition.OptionsFunction<String> getFirstMessages() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            ChatMemoryRepository chatMemoryRepository = getChatMemoryRepository(connectionParameters);

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            List<String> conversationIds = chatMemoryRepository.findConversationIds();

            for (String conversationId : conversationIds) {
                List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

                Message message = messages.getFirst();

                options.add(option(conversationId, conversationId, message.getText()));
            }

            return options;
        };
    }
}
