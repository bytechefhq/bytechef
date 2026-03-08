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

import static com.bytechef.component.ai.agent.chat.memory.neo4j.constant.Neo4jChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.neo4j.constant.Neo4jChatMemoryConstants.URI;
import static com.bytechef.component.ai.agent.chat.memory.neo4j.constant.Neo4jChatMemoryConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.neo4j.Neo4jChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.neo4j.Neo4jChatMemoryRepositoryConfig;

/**
 * @author Ivica Cardic
 */
public class Neo4jChatMemoryUtils {

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        String uri = connectionParameters.getRequiredString(URI);
        String username = connectionParameters.getString(USERNAME);
        String password = connectionParameters.getString(PASSWORD);

        Driver driver;

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        } else {
            driver = GraphDatabase.driver(uri);
        }

        Neo4jChatMemoryRepositoryConfig config = Neo4jChatMemoryRepositoryConfig.builder()
            .withDriver(driver)
            .build();

        return new Neo4jChatMemoryRepository(config);
    }

    private Neo4jChatMemoryUtils() {
    }
}
