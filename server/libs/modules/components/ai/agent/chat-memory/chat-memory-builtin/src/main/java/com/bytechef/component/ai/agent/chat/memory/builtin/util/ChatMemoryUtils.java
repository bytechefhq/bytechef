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

package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Marko Kriskovic
 */
public class ChatMemoryUtils {

    private ChatMemoryUtils() {
    }

    public static ActionDefinition.OptionsFunction<String> getFirstMessages(ChatMemoryRepository chatMemoryRepository) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            if (chatMemoryRepository == null) {
                return List.of();
            }

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            List<String> conversationIds = chatMemoryRepository.findConversationIds();

            for (String conversationId : conversationIds) {
                List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

                Message message = messages.getFirst();

                options.add(option(conversationId, conversationId, message.getText()));
            }

            return options;
        };
    }
}
