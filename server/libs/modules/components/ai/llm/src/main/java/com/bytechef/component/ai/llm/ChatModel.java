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

package com.bytechef.component.ai.llm;

import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;

import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.lang.Nullable;

/**
 * @author Marko Kriskovic
 * @author Monika Kušter
 */
@FunctionalInterface
public interface ChatModel {

    enum ResponseFormat {
        TEXT, JSON
    }

    enum Role {
        ASSISTANT, SYSTEM, /* TOOL, */ USER
    }

    enum Format {
        SIMPLE, ADVANCED
    }

    org.springframework.ai.chat.model.ChatModel createChatModel(
        Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired);

    @Nullable
    default Object getResponse(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext, boolean messageFormatRequired) {

        org.springframework.ai.chat.model.ChatModel chatModel = createChatModel(
            inputParameters, connectionParameters, true);

        List<org.springframework.ai.chat.messages.Message> messages = ModelUtils.getMessages(
            inputParameters, actionContext, messageFormatRequired);

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = createPrompt(
            inputParameters, actionContext, chatModel);

        ChatClient.CallResponseSpec callResponseSpec = chatClientRequestSpec
            .messages(messages)
            .advisors(
                SimpleLoggerAdvisor.builder()
                    .build())
            .call();

        return ModelUtils.getChatResponse(callResponseSpec, inputParameters, actionContext);
    }

    private ChatClient.ChatClientRequestSpec createPrompt(
        Parameters inputParameters, ActionContext actionContext,
        org.springframework.ai.chat.model.ChatModel chatModel) {

        ChatClient chatClient = ChatClient.create(chatModel);

        ResponseFormat responseFormat = inputParameters.getRequiredFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class);

        if (responseFormat.equals(ResponseFormat.TEXT)) {
            return chatClient.prompt();
        } else {
            JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
                inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), actionContext);

            return chatClient.prompt(converter.getFormat());
        }
    }

    @SuppressFBWarnings("EI")
    record Message(String content, @Nullable List<FileEntry> attachments, Role role) {
    }
}
