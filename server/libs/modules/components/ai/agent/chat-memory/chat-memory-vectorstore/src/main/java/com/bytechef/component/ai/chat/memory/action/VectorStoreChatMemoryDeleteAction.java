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

package com.bytechef.component.ai.chat.memory.action;

import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_CONVERSATION_ID;
import static com.bytechef.component.ai.chat.memory.util.VectorStoreChatMemoryUtils.getVectorStore;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryDeleteAction {

    public static ActionDefinition getActionDefinition(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return action("deleteConversation")
            .title("Delete Conversation")
            .description("Deletes all messages for a conversation from the vector store.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation to delete.")
                    .required(true))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            bool("deleted"))))
            .perform(
                (MultipleConnectionsPerformFunction) (
                    inputParameters, componentConnections, extensions, context) -> perform(inputParameters,
                        componentConnections, extensions, clusterElementDefinitionService));
    }

    private VectorStoreChatMemoryDeleteAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        Parameters extensions, ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        VectorStore vectorStore = getVectorStore(extensions, componentConnections, clusterElementDefinitionService);

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression filterExpression = filterExpressionBuilder
            .eq(METADATA_CONVERSATION_ID, conversationId)
            .build();

        vectorStore.delete(filterExpression);

        return Map.of(
            CONVERSATION_ID, conversationId,
            "deleted", true);
    }
}
