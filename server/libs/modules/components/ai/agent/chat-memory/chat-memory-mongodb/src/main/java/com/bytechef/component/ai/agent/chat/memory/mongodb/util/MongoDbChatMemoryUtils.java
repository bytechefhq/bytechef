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

package com.bytechef.component.ai.agent.chat.memory.mongodb.util;

import static com.bytechef.component.ai.agent.chat.memory.mongodb.constant.MongoDbChatMemoryConstants.CONNECTION_STRING;
import static com.bytechef.component.ai.agent.chat.memory.mongodb.constant.MongoDbChatMemoryConstants.DATABASE_NAME;

import com.bytechef.component.definition.Parameters;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * @author Ivica Cardic
 */
public class MongoDbChatMemoryUtils {

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        String connectionString = connectionParameters.getRequiredString(CONNECTION_STRING);
        String databaseName = connectionParameters.getString(DATABASE_NAME, "spring_ai");

        MongoClient mongoClient = MongoClients.create(connectionString);

        MongoTemplate mongoTemplate = new MongoTemplate(
            new SimpleMongoClientDatabaseFactory(mongoClient, databaseName));

        return MongoChatMemoryRepository.builder()
            .mongoTemplate(mongoTemplate)
            .build();
    }

    private MongoDbChatMemoryUtils() {
    }
}
