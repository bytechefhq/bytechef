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

package com.bytechef.component.ai.agent.chat.memory.redis;

import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.HOST;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.TIME_TO_LIVE;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.redis.cluster.RedisChatMemory;
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
public class RedisChatMemoryComponentHandler implements ComponentHandler {

    private static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(HOST)
                .label("Host")
                .description("The Redis server host.")
                .defaultValue("localhost")
                .required(true),
            integer(PORT)
                .label("Port")
                .description("The Redis server port.")
                .defaultValue(6379)
                .required(true),
            string(KEY_PREFIX)
                .label("Key Prefix")
                .description("The prefix for Redis keys used to store chat messages.")
                .defaultValue("bytechef-chat-memory:")
                .required(false),
            string(TIME_TO_LIVE)
                .label("Time to Live")
                .description(
                    "The time-to-live for chat messages (e.g., '24h', '7d', '30m'). Leave empty for no expiration.")
                .required(false))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("The Redis username (optional, for Redis 6.0+ ACL).")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("The Redis password.")
                        .controlType(ControlType.PASSWORD)
                        .required(false)));

    private static final ComponentDefinition COMPONENT_DEFINITION = component("redisChatMemory")
        .title("Redis Chat Memory")
        .description("Redis Chat Memory stores conversation history in Redis for fast, persistent storage.")
        .icon("path:assets/redis-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            RedisChatMemoryAddMessagesAction.ACTION_DEFINITION,
            RedisChatMemoryGetMessagesAction.ACTION_DEFINITION,
            RedisChatMemoryDeleteAction.ACTION_DEFINITION,
            RedisChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(RedisChatMemory.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
