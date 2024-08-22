package com.bytechef.component.llm.util.interfaces;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.records.MessageRecord;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static com.bytechef.component.llm.util.LLMUtils.createMessage;

public interface Chat {
    private static List<Message> getMessages(Parameters inputParameters) {
        List<MessageRecord> messageRecordList = inputParameters.getList(MESSAGES, new Context.TypeReference<>() {});
        return messageRecordList.stream()
            .map(messageRecord -> createMessage(messageRecord.getRole(), messageRecord.getContent()))
            .toList();
    }

    static String getResponse(Chat chat, Parameters inputParameters, Parameters connectionParameters) {
        ChatModel chatModel = chat.createChatModel(inputParameters, connectionParameters);

        List<Message> messages = Chat.getMessages(inputParameters);

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult()
            .getOutput()
            .getContent();
    }
    ChatOptions createChatOptions(Parameters inputParameters);
    ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters);
}
