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

package com.bytechef.component.ai.agent.chat.memory.cosmosdb;

import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.CONTAINER_NAME;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.DATABASE_NAME;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.ENDPOINT;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.cosmosdb.action.CosmosDbChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.cosmosdb.action.CosmosDbChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.cosmosdb.action.CosmosDbChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.cosmosdb.action.CosmosDbChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.cosmosdb.cluster.CosmosDbChatMemory;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class CosmosDbChatMemoryComponentHandler implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(ENDPOINT)
                .label("Endpoint")
                .description("The Azure Cosmos DB account endpoint URI.")
                .required(true),
            string(DATABASE_NAME)
                .label("Database Name")
                .description("The database name.")
                .defaultValue("spring_ai")
                .required(false),
            string(CONTAINER_NAME)
                .label("Container Name")
                .description("The container name for storing chat memory.")
                .defaultValue("chat_memory")
                .required(false))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(KEY)
                        .label("Key")
                        .description("The Azure Cosmos DB account key.")
                        .controlType(ControlType.PASSWORD)
                        .required(true)));

    private static final ComponentDefinition COMPONENT_DEFINITION = component("cosmosDbChatMemory")
        .title("Cosmos DB Chat Memory")
        .description(
            "Cosmos DB Chat Memory stores conversation history in Azure Cosmos DB for globally distributed, " +
                "scalable persistent storage.")
        .icon("path:assets/cosmosdb-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            CosmosDbChatMemoryAddMessagesAction.ACTION_DEFINITION,
            CosmosDbChatMemoryGetMessagesAction.ACTION_DEFINITION,
            CosmosDbChatMemoryDeleteAction.ACTION_DEFINITION,
            CosmosDbChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(CosmosDbChatMemory.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
