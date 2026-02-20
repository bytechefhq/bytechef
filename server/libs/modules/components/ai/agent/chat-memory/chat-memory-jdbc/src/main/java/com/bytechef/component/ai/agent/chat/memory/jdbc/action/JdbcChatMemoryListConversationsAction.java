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

package com.bytechef.component.ai.agent.chat.memory.jdbc.action;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.util.JdbcChatMemoryUtils.getChatMemoryRepository;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

/**
 * @author Ivica Cardic
 */
public class JdbcChatMemoryListConversationsAction {

    public static ActionDefinition getActionDefinition(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return action("listConversations")
            .title("List Conversations")
            .description("Lists all conversation IDs in the chat memory.")
            .output(
                outputSchema(
                    object()
                        .properties(
                            array("conversationIds")
                                .items(string()),
                            integer("count"))))
            .perform(
                (MultipleConnectionsPerformFunction) (
                    inputParameters, componentConnections, extensions, context) -> perform(inputParameters,
                        componentConnections, extensions, clusterElementDefinitionService));
    }

    private JdbcChatMemoryListConversationsAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        Parameters extensions, ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        ChatMemoryRepository repository = getChatMemoryRepository(
            extensions, componentConnections, clusterElementDefinitionService);

        List<String> conversationIds = repository.findConversationIds();

        return Map.of(
            "conversationIds", conversationIds,
            "count", conversationIds.size());
    }
}
