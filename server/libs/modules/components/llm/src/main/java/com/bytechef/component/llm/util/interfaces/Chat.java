/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.llm.util.interfaces;

import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static com.bytechef.component.llm.util.LLMUtils.createMessage;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.records.MessageRecord;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

public interface Chat {
    private static List<Message> getMessages(Parameters inputParameters) {
        List<MessageRecord> messageRecordList = inputParameters.getList(MESSAGES, new Context.TypeReference<>() {});
        return messageRecordList.stream()
            .map(messageRecord -> createMessage(messageRecord.getRole(), messageRecord.getContent()))
            .toList();
    }

    static String getResponse(Chat chat, Parameters inputParameters, Parameters connectionParameters) {
        ChatModel chatModel = chat.createChatModel(inputParameters, connectionParameters);

        List<Message> messages = getMessages(inputParameters);

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult()
            .getOutput()
            .getContent();
    }

    ChatOptions createChatOptions(Parameters inputParameters);

    ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters);
}
