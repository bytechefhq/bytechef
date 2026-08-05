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

package com.bytechef.component.ai.agent.chat.memory.builtin.action;

import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.ChatMemoryUtils;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class ChatMemoryDeleteAction {

    public static ModifiableActionDefinition of(SessionRepository sessionRepository) {
        return action("deleteConversation")
            .title("Delete Conversation")
            .description("Deletes all messages for a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation to delete.")
                    .options(ChatMemoryUtils.getFirstMessages(sessionRepository))
                    .required(true))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            bool("deleted"))))
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, sessionRepository));
    }

    private ChatMemoryDeleteAction() {
    }

    protected static Object perform(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        boolean existed = Optional.ofNullable(sessionRepository.findById(conversationId))
            .isPresent();

        sessionRepository.delete(conversationId);

        return Map.of(
            CONVERSATION_ID, conversationId,
            "deleted", existed);
    }
}
