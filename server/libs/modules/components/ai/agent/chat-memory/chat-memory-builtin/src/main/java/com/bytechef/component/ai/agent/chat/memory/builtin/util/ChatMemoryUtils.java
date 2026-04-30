package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import com.bytechef.component.definition.ComponentDsl;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

import static com.bytechef.component.definition.ComponentDsl.option;

public class ChatMemoryUtils {
    public static List<ComponentDsl.ModifiableOption<String>> getFirstMessages(ChatMemoryRepository chatMemoryRepository) {
        List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

        List<String> conversationIds = chatMemoryRepository.findConversationIds();
        for (String conversationId : conversationIds) {
            List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
            options.add(option(conversationId, conversationId, messages.getFirst().getText()));
        }

        return options;
    }
}
