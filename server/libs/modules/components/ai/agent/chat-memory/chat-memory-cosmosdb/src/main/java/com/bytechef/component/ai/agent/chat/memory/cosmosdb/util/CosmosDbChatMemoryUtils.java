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

package com.bytechef.component.ai.agent.chat.memory.cosmosdb.util;

import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.CONTAINER_NAME;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.DATABASE_NAME;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.ENDPOINT;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.KEY;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cosmosdb.CosmosDBChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cosmosdb.CosmosDBChatMemoryRepositoryConfig;

/**
 * @author Ivica Cardic
 */
public class CosmosDbChatMemoryUtils {

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        String endpoint = connectionParameters.getRequiredString(ENDPOINT);
        String key = connectionParameters.getRequiredString(KEY);
        String databaseName = connectionParameters.getString(DATABASE_NAME, "spring_ai");
        String containerName = connectionParameters.getString(CONTAINER_NAME, "chat_memory");

        CosmosAsyncClient cosmosAsyncClient = new CosmosClientBuilder()
            .endpoint(endpoint)
            .key(key)
            .buildAsyncClient();

        CosmosDBChatMemoryRepositoryConfig config = CosmosDBChatMemoryRepositoryConfig.builder()
            .withCosmosClient(cosmosAsyncClient)
            .withDatabaseName(databaseName)
            .withContainerName(containerName)
            .build();

        return CosmosDBChatMemoryRepository.create(config);
    }

    private CosmosDbChatMemoryUtils() {
    }
}
