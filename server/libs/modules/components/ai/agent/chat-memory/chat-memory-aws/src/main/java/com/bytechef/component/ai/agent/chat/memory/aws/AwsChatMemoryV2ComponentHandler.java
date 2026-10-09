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

package com.bytechef.component.ai.agent.chat.memory.aws;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.AWS_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.aws.connection.AwsChatMemoryConnection;
import com.bytechef.component.ai.agent.chat.memory.aws.util.AwsSessionChatMemoryUtils;
import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.ai.agent.chat.memory.session.action.SessionChatMemoryActions;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class AwsChatMemoryV2ComponentHandler implements ComponentHandler {

    private static final SessionRepositoryResolver SESSION_REPOSITORY_RESOLVER =
        (inputParameters, connectionParameters, extensions, componentConnections) -> AwsSessionChatMemoryUtils
            .getSessionRepository(connectionParameters);

    private static final SessionChatMemoryComponentDefinition COMPONENT_DEFINITION =
        new AwsChatMemoryV2ComponentDefinition(
            component(AWS_CHAT_MEMORY)
                .title("AWS S3 Chat Memory")
                .description("Stores session-based conversation history as JSON objects in an Amazon S3 bucket.")
                .icon("path:assets/aws-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .version(2)
                .connection(AwsChatMemoryConnection.CONNECTION_DEFINITION)
                .actions(SessionChatMemoryActions.of("aws-chat-memory", SESSION_REPOSITORY_RESOLVER, false))
                .clusterElements(SessionChatMemory.of("AWS S3 Chat Memory", SESSION_REPOSITORY_RESOLVER, null)));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

    private static class AwsChatMemoryV2ComponentDefinition extends AbstractComponentDefinitionWrapper
        implements SessionChatMemoryComponentDefinition {

        AwsChatMemoryV2ComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
