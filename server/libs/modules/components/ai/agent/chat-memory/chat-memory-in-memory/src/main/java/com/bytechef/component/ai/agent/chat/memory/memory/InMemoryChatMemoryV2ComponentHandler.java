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

package com.bytechef.component.ai.agent.chat.memory.memory;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.ai.agent.chat.memory.session.action.SessionChatMemoryActions;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.definition.ai.agent.TenantRoutingSessionRepository;
import com.google.auto.service.AutoService;
import org.springframework.ai.session.InMemorySessionRepository;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class InMemoryChatMemoryV2ComponentHandler implements ComponentHandler {

    private final SessionChatMemoryComponentDefinition componentDefinition;

    public InMemoryChatMemoryV2ComponentHandler() {
        TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> InMemorySessionRepository.builder()
                .build());

        SessionRepositoryResolver sessionRepositoryResolver =
            (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository;

        this.componentDefinition = new InMemoryChatMemoryV2ComponentDefinition(
            component("inMemoryChatMemory")
                .title("In Memory Chat Memory")
                .description("In Memory session-based Chat Memory.")
                .icon("path:assets/in-memory-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .version(2)
                .actions(SessionChatMemoryActions.of("in-memory-chat-memory", sessionRepositoryResolver, false))
                .clusterElements(SessionChatMemory.of("In Memory Chat Memory", sessionRepositoryResolver, null)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class InMemoryChatMemoryV2ComponentDefinition extends AbstractComponentDefinitionWrapper
        implements SessionChatMemoryComponentDefinition {

        InMemoryChatMemoryV2ComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
