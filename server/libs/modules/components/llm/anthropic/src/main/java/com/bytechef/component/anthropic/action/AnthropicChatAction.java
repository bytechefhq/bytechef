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

package com.bytechef.component.anthropic.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.llm.util.LLMUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * @author Marko Kriskovic
 */
public class AnthropicChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(AnthropicApi.ChatModel.values())
                            .collect(
                                Collectors.toMap(
                                    AnthropicApi.ChatModel::getValue, AnthropicApi.ChatModel::getValue, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            integer(MAX_TOKENS)
                .label("Max Tokens")
                .description("The maximum number of tokens to generate in the chat completion.")
                .required(true),
            RESPONSE_FORMAT_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY,
            FUNCTIONS_PROPERTY)
        .output()
        .perform(AnthropicChatAction::perform);

    private AnthropicChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {

        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getDouble(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getDouble(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K));

            List<String> functions = inputParameters.getList(FUNCTIONS, new TypeReference<>() {});

            if (functions != null) {
                builder.withFunctions(new HashSet<>(functions));
            }

            return builder.build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new AnthropicChatModel(
                new AnthropicApi(connectionParameters.getString(TOKEN)),
                (AnthropicChatOptions) createChatOptions(inputParameters));
        }
    };
}
