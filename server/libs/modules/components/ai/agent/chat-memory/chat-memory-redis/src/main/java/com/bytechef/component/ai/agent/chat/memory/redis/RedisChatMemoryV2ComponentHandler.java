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
import com.bytechef.component.ai.agent.chat.memory.redis.connection.RedisChatMemoryConnection;
import com.bytechef.component.ai.agent.chat.memory.redis.util.RedisSessionChatMemoryUtils;
import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.ai.agent.chat.memory.session.action.SessionChatMemoryActions;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("redisChatMemory_v2_ComponentHandler")
public class RedisChatMemoryV2ComponentHandler implements ComponentHandler {

    private final SessionChatMemoryComponentDefinition componentDefinition;

    public RedisChatMemoryV2ComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        SessionRepositoryResolver sessionRepositoryResolver =
            (inputParameters, connectionParameters, extensions, componentConnections) -> RedisSessionChatMemoryUtils
                .getSessionRepository(connectionParameters);

        this.componentDefinition = new RedisChatMemoryV2ComponentDefinition(
            component("redisChatMemory")
                .title("Redis Chat Memory")
                .description("Redis Chat Memory stores session-based conversation history in Redis.")
                .icon("path:assets/redis-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .version(2)
                .connection(RedisChatMemoryConnection.CONNECTION_DEFINITION)
                .actions(SessionChatMemoryActions.of("redis-chat-memory", sessionRepositoryResolver, false))
                .clusterElements(
                    SessionChatMemory.of(
                        "Redis Chat Memory", sessionRepositoryResolver, clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class RedisChatMemoryV2ComponentDefinition extends AbstractComponentDefinitionWrapper
        implements SessionChatMemoryComponentDefinition {

        RedisChatMemoryV2ComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
