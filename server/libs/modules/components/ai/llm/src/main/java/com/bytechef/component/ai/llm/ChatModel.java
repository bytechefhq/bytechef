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

package com.bytechef.component.ai.llm;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.util.LLMUtils.createMessage;

import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * @author Marko Kriskovic
 */
public interface ChatModel {

    org.springframework.ai.chat.model.ChatModel createChatModel(
        Parameters inputParameters, Parameters connectionParameters);

    default Object getResponse(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        org.springframework.ai.chat.model.ChatModel chatModel = createChatModel(inputParameters, connectionParameters);

        List<org.springframework.ai.chat.messages.Message> messages = getMessages(inputParameters, actionContext);

        ChatClient.CallResponseSpec call = ChatClient.create(chatModel)
            .prompt()
            .messages(messages)
            .call();

        return returnChatEntity(inputParameters, call, actionContext);
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
    private Object returnChatEntity(
        Parameters parameters, ChatClient.CallResponseSpec call, ActionContext actionContext) {

        int responseFormat = parameters.getFromPath(RESPONSE + "." + RESPONSE_FORMAT, Integer.class, 1);

        if (responseFormat == 1) {
            try {
                return Objects.requireNonNull(call.chatResponse())
                    .getResult()
                    .getOutput()
                    .getContent();
            } catch (org.springframework.ai.retry.NonTransientAiException e) {
                String message = e.getMessage();

                String providerMessage = actionContext.json(
                    json -> json.read(
                        message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1), "error.message",
                        new TypeReference<>() {}));

                throw new ProviderException(providerMessage);
            }
        } else {
            return call.entity(
                new JsonSchemaStructuredOutputConverter(
                    parameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), actionContext));
        }
    }

    @SuppressFBWarnings("EI")
    record Message(String content, List<FileEntry> attachments, String role) {
    }
}
