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

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.redis.action.RedisChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.redis.cluster.RedisChatMemory;
import com.bytechef.component.ai.agent.chat.memory.redis.connection.RedisChatMemoryConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RedisChatMemoryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("redisChatMemory")
        .title("Redis Chat Memory")
        .description("Redis Chat Memory stores conversation history in Redis for fast, persistent storage.")
        .icon("path:assets/redis-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(RedisChatMemoryConnection.CONNECTION_DEFINITION)
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
