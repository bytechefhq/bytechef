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

package com.bytechef.platform.component.definition.ai.agent;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.definition.ai.agent.ConversationFirstMessageReader.ConversationFirstMessage;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * Builds the bounded conversation options of the chat memory components: at most {@link #MAX_OPTIONS} conversations, in
 * the order the repository returns them, labelled by their first message.
 *
 * @author Ivica Cardic
 */
public final class ChatMemoryConversationOptions {

    public static final int MAX_OPTIONS = 100;

    private ChatMemoryConversationOptions() {
    }

    public static List<Option<String>> getOptions(
        ChatMemoryRepository chatMemoryRepository, @Nullable String searchText) {

        String normalizedSearchText = StringUtils.isBlank(searchText) ? null : searchText.trim();

        if (chatMemoryRepository instanceof ConversationFirstMessageReader conversationFirstMessageReader) {
            List<Option<String>> options = new ArrayList<>();

            for (ConversationFirstMessage conversationFirstMessage : conversationFirstMessageReader.findFirstMessages(
                normalizedSearchText, MAX_OPTIONS)) {

                options.add(toOption(conversationFirstMessage.conversationId(), conversationFirstMessage.text()));
            }

            return options;
        }

        List<String> conversationIds = chatMemoryRepository.findConversationIds()
            .stream()
            .filter(conversationId -> normalizedSearchText == null ||
                Strings.CI.contains(conversationId, normalizedSearchText))
            .limit(MAX_OPTIONS)
            .toList();

        List<Option<String>> options = new ArrayList<>();

        for (String conversationId : conversationIds) {
            List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

            if (messages.isEmpty()) {
                continue;
            }

            Message message = messages.getFirst();

            options.add(toOption(conversationId, message.getText()));
        }

        return options;
    }

    private static Option<String> toOption(String conversationId, @Nullable String text) {
        return option(conversationId, conversationId, text);
    }
}
