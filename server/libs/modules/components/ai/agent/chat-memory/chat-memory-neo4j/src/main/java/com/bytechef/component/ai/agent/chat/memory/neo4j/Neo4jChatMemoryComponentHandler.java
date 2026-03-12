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

package com.bytechef.component.ai.agent.chat.memory.neo4j;

import static com.bytechef.component.ai.agent.chat.memory.neo4j.constant.Neo4jChatMemoryConstants.URI;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.neo4j.action.Neo4jChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.neo4j.action.Neo4jChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.neo4j.action.Neo4jChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.neo4j.action.Neo4jChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.neo4j.cluster.Neo4jChatMemory;
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
public class Neo4jChatMemoryComponentHandler implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(URI)
                .label("URI")
                .description("The Neo4j server URI.")
                .defaultValue("bolt://localhost:7687")
                .required(true))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("The Neo4j username.")
                        .defaultValue("neo4j")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("The Neo4j password.")
                        .controlType(ControlType.PASSWORD)
                        .required(false)));

    private static final ComponentDefinition COMPONENT_DEFINITION = component("neo4jChatMemory")
        .title("Neo4j Chat Memory")
        .description(
            "Neo4j Chat Memory stores conversation history in Neo4j graph database for persistent storage with graph-based relationships.")
        .icon("path:assets/neo4j-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            Neo4jChatMemoryAddMessagesAction.ACTION_DEFINITION,
            Neo4jChatMemoryGetMessagesAction.ACTION_DEFINITION,
            Neo4jChatMemoryDeleteAction.ACTION_DEFINITION,
            Neo4jChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(Neo4jChatMemory.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
