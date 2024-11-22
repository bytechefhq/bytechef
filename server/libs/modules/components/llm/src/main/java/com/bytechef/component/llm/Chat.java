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

package com.bytechef.component.llm;

import static com.bytechef.component.llm.constant.LLMConstants.MESSAGES;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.llm.util.LLMUtils.createMessage;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * @author Marko Kriskovic
 */
public interface Chat {

    static Object getResponse(
        Chat chat, Parameters inputParameters, Parameters connectionParameters) {

        ChatModel chatModel = chat.createChatModel(inputParameters, connectionParameters);

        List<org.springframework.ai.chat.messages.Message> messages = getMessages(inputParameters);

        ChatClient.CallResponseSpec call = ChatClient.create(chatModel)
            .prompt()
            .messages(messages)
            .call();

        return returnChatEntity(inputParameters.getInteger(RESPONSE_FORMAT), call);
    }

    ChatOptions createChatOptions(Parameters inputParameters);

    ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters);

    private static List<org.springframework.ai.chat.messages.Message> getMessages(
        Parameters inputParameters) {

        List<Message> messages = inputParameters.getList(MESSAGES, new TypeReference<>() {});

        List<org.springframework.ai.chat.messages.Message> list = new java.util.ArrayList<>(messages.stream()
            .map(message -> createMessage(message.role(), message.content()))
            .toList());

        String responseSchema = inputParameters.getString(RESPONSE_SCHEMA);

        if (responseSchema != null && !responseSchema.isEmpty()) {
            list.add(new SystemMessage(responseSchema));
        }

        return list;
    }

    @SuppressFBWarnings("NP")
    private static Object returnChatEntity(Integer integer, ChatClient.CallResponseSpec call) {
        return switch (integer) {
            case 1 -> call.entity(new ParameterizedTypeReference<Map<String, Object>>() {});
            case 2 -> call.entity(new ListOutputConverter(new DefaultConversionService()));
            case null, default -> call.chatResponse()
                .getResult()
                .getOutput()
                .getContent();
        };
    }

    record Message(String content, String role) {
    }
}
