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

package com.bytechef.ai.copilot.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
final class ChatMemoryRewinder {

    private ChatMemoryRewinder() {
    }

    static void rewind(ChatMemory chatMemory, String conversationId, List<String> previousUserTexts) {
        List<Message> messages = chatMemory.get(conversationId);

        List<Integer> userMessageIndexes = new ArrayList<>();

        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index) instanceof UserMessage) {
                userMessageIndexes.add(index);
            }
        }

        if (userMessageIndexes.isEmpty()) {
            return;
        }

        if (previousUserTexts.isEmpty()) {
            chatMemory.clear(conversationId);

            return;
        }

        int keptUserMessageCount = findKeptUserMessageCount(messages, userMessageIndexes, previousUserTexts);

        if (keptUserMessageCount <= 0 || keptUserMessageCount == userMessageIndexes.size()) {
            return;
        }

        List<Message> keptMessages = new ArrayList<>(
            messages.subList(0, userMessageIndexes.get(keptUserMessageCount)));

        chatMemory.clear(conversationId);

        chatMemory.add(conversationId, keptMessages);
    }

    private static int findKeptUserMessageCount(
        List<Message> messages, List<Integer> userMessageIndexes, List<String> previousUserTexts) {

        for (int keptCount = userMessageIndexes.size(); keptCount > 0; keptCount--) {
            int comparedCount = Math.min(keptCount, previousUserTexts.size());

            boolean matches = true;

            for (int offset = 1; offset <= comparedCount; offset++) {
                Message message = messages.get(userMessageIndexes.get(keptCount - offset));

                if (!Objects.equals(message.getText(), previousUserTexts.get(previousUserTexts.size() - offset))) {
                    matches = false;

                    break;
                }
            }

            if (matches) {
                return keptCount;
            }
        }

        return 0;
    }
}
