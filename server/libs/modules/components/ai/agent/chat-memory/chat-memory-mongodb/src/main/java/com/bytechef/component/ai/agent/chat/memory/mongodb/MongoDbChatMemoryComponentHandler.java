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

package com.bytechef.component.ai.agent.chat.memory.mongodb;

import static com.bytechef.component.ai.agent.chat.memory.mongodb.constant.MongoDbChatMemoryConstants.CONNECTION_STRING;
import static com.bytechef.component.ai.agent.chat.memory.mongodb.constant.MongoDbChatMemoryConstants.DATABASE_NAME;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.mongodb.action.MongoDbChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.mongodb.action.MongoDbChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.mongodb.action.MongoDbChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.mongodb.action.MongoDbChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.mongodb.cluster.MongoDbChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class MongoDbChatMemoryComponentHandler implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(CONNECTION_STRING)
                .label("Connection String")
                .description("The MongoDB connection string.")
                .defaultValue("mongodb://localhost:27017")
                .required(true),
            string(DATABASE_NAME)
                .label("Database Name")
                .description("The MongoDB database name.")
                .defaultValue("spring_ai")
                .required(false));

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mongoDbChatMemory")
        .title("MongoDB Chat Memory")
        .description(
            "MongoDB Chat Memory stores conversation history in MongoDB for flexible, document-based persistent storage.")
        .icon("path:assets/mongodb-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            MongoDbChatMemoryAddMessagesAction.ACTION_DEFINITION,
            MongoDbChatMemoryGetMessagesAction.ACTION_DEFINITION,
            MongoDbChatMemoryDeleteAction.ACTION_DEFINITION,
            MongoDbChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(MongoDbChatMemory.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
