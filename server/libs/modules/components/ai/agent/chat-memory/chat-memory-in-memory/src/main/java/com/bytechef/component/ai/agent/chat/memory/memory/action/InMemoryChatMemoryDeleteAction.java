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

package com.bytechef.component.ai.agent.chat.memory.memory.action;

import static com.bytechef.component.ai.agent.chat.memory.memory.constant.InMemoryChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.memory.InMemoryChatMemoryRepositoryHolder;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

/**
 * @author Ivica Cardic
 */
public class InMemoryChatMemoryDeleteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteConversation")
        .title("Delete Conversation")
        .description("Deletes all messages for a conversation.")
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
        .perform(InMemoryChatMemoryDeleteAction::perform);

    private InMemoryChatMemoryDeleteAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        ChatMemoryRepository repository = InMemoryChatMemoryRepositoryHolder.getInstance();

        repository.deleteByConversationId(conversationId);

        return Map.of(
            CONVERSATION_ID, conversationId,
            "deleted", true);
    }
}
