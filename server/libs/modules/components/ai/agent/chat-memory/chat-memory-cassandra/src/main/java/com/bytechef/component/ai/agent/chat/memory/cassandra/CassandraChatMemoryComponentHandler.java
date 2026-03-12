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

package com.bytechef.component.ai.agent.chat.memory.cassandra;

import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.CONTACT_POINTS;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.DATACENTER;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.KEYSPACE;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.TABLE;
import static com.bytechef.component.ai.agent.chat.memory.cassandra.constant.CassandraChatMemoryConstants.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.cassandra.action.CassandraChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.cassandra.action.CassandraChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.cassandra.action.CassandraChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.cassandra.action.CassandraChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.cassandra.cluster.CassandraChatMemory;
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
public class CassandraChatMemoryComponentHandler implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(CONTACT_POINTS)
                .label("Contact Points")
                .description("The Cassandra contact points (hostname or IP).")
                .defaultValue("localhost")
                .required(true),
            integer(PORT)
                .label("Port")
                .description("The Cassandra native transport port.")
                .defaultValue(9042)
                .required(true),
            string(DATACENTER)
                .label("Datacenter")
                .description("The local datacenter name.")
                .defaultValue("datacenter1")
                .required(true),
            string(KEYSPACE)
                .label("Keyspace")
                .description("The Cassandra keyspace to use.")
                .required(false),
            string(TABLE)
                .label("Table")
                .description("The table name for storing chat memory.")
                .defaultValue("chat_memory")
                .required(false))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("The Cassandra username (optional).")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("The Cassandra password.")
                        .controlType(ControlType.PASSWORD)
                        .required(false)));

    private static final ComponentDefinition COMPONENT_DEFINITION = component("cassandraChatMemory")
        .title("Cassandra Chat Memory")
        .description(
            "Cassandra Chat Memory stores conversation history in Apache Cassandra for distributed, scalable " +
                "persistent storage.")
        .icon("path:assets/cassandra-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            CassandraChatMemoryAddMessagesAction.ACTION_DEFINITION,
            CassandraChatMemoryGetMessagesAction.ACTION_DEFINITION,
            CassandraChatMemoryDeleteAction.ACTION_DEFINITION,
            CassandraChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(CassandraChatMemory.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
