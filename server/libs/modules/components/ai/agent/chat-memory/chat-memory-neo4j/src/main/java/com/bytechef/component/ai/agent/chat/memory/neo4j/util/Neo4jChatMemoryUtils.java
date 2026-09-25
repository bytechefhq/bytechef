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

package com.bytechef.component.ai.agent.chat.memory.neo4j.util;

import static com.bytechef.component.ai.agent.chat.memory.neo4j.constant.Neo4jChatMemoryConstants.URI;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
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
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.neo4j.Neo4jChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.neo4j.Neo4jChatMemoryRepositoryConfig;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Ivica Cardic
 */
public class Neo4jChatMemoryUtils {

    private static final Duration CLIENT_IDLE_TIMEOUT = Duration.ofHours(24);

    private static final Cache<DriverKey, Driver> CLIENTS = Caffeine.newBuilder()
        .expireAfterAccess(CLIENT_IDLE_TIMEOUT)
        .removalListener((DriverKey driverKey, Driver driver, RemovalCause removalCause) -> {
            if (driver != null) {
                driver.close();
            }
        })
        .build();

    private Neo4jChatMemoryUtils() {
    }

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        Neo4jChatMemoryRepositoryConfig config = Neo4jChatMemoryRepositoryConfig.builder()
            .withDriver(getSharedDriver(connectionParameters))
            .build();

        return new Neo4jChatMemoryRepository(config);
    }

    static Driver getSharedDriver(Parameters connectionParameters) {
        return CLIENTS.get(toDriverKey(connectionParameters), Neo4jChatMemoryUtils::buildDriver);
    }

    private static Driver buildDriver(DriverKey driverKey) {
        String username = driverKey.username();
        String password = driverKey.password();

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            return GraphDatabase.driver(driverKey.uri(), AuthTokens.basic(username, password));
        }

        return GraphDatabase.driver(driverKey.uri());
    }

    private static DriverKey toDriverKey(Parameters connectionParameters) {
        return new DriverKey(
            connectionParameters.getRequiredString(URI), connectionParameters.getString(USERNAME),
            connectionParameters.getString(PASSWORD));
    }

    private record DriverKey(String uri, @Nullable String username, @Nullable String password) {

        @Override
        public String toString() {
            return "DriverKey{uri=" + uri + ", username=" + username + "}";
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
