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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.llm.converter.JsonSchemaStructuredOutputConverter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Marko Kriskovic
 */
public interface Chat {

    ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters);

    default Object getResponse(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ChatModel chatModel = createChatModel(inputParameters, connectionParameters);

        List<org.springframework.ai.chat.messages.Message> messages = getMessages(inputParameters, actionContext);

        ChatClient.CallResponseSpec call = ChatClient.create(chatModel)
            .prompt()
            .messages(messages)
            .call();

        return returnChatEntity(inputParameters, call, context);
    }

    private List<org.springframework.ai.chat.messages.Message> getMessages(
        Parameters inputParameters, ActionContext actionContext) {

        List<Message> messages = inputParameters.getList(MESSAGES, new TypeReference<>() {});

        List<org.springframework.ai.chat.messages.Message> list = new java.util.ArrayList<>(messages.stream()
            .map(message -> createMessage(message, actionContext))
            .toList());

        String responseSchema = inputParameters.getString(RESPONSE_SCHEMA);

        if (responseSchema != null && !responseSchema.isEmpty()) {
            list.add(new SystemMessage(responseSchema));
        }

        return list;
    }

    @SuppressFBWarnings("NP")
    private Object returnChatEntity(Parameters parameters, ChatClient.CallResponseSpec call, Context context) {
        int responseFormat = parameters.getInteger(RESPONSE_FORMAT, 0);

        if (responseFormat == 0) {
            return Objects.requireNonNull(call.chatResponse())
                .getResult()
                .getOutput()
                .getContent();
        } else {
            return call.entity(
                new JsonSchemaStructuredOutputConverter(parameters.getRequiredString(RESPONSE_SCHEMA), context));
        }
    }

    record Message(String content, FileEntry image, String role) {
    }
}
