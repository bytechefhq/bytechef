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

package com.bytechef.component.mistral.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.mistral.constant.MistralConstants.SAFE_PROMPT;

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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;

/**
 * @author Marko Kriskovic
 */
public class MistralChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(
                        MistralAiApi.ChatModel.values())
                        .collect(
                            Collectors.toMap(
                                MistralAiApi.ChatModel::getValue, MistralAiApi.ChatModel::getValue, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            STOP_PROPERTY,
            FUNCTIONS_PROPERTY,
            SEED_PROPERTY,
            bool(SAFE_PROMPT)
                .label("Safe prompt")
                .description("Should the prompt be safe for work?")
                .defaultValue(true)
                .advancedOption(true))
        .output()
        .perform(MistralChatAction::perform);

    private MistralChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {

        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            Integer responseInteger = inputParameters.getInteger(RESPONSE_FORMAT);
            String type = responseInteger != null ? responseInteger < 1 ? "text" : "json_object" : null;

            MistralAiChatOptions.Builder builder = MistralAiChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withSafePrompt(inputParameters.getBoolean(SAFE_PROMPT))
                .withRandomSeed(inputParameters.getInteger(SEED))
                .withResponseFormat(new MistralAiApi.ChatCompletionRequest.ResponseFormat(type));

            List<String> functions = inputParameters.getList(FUNCTIONS, new TypeReference<>() {});

            if (functions != null) {
                builder.withFunctions(new HashSet<>(functions));
            }

            return builder.build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new MistralAiChatModel(
                new MistralAiApi(connectionParameters.getString(TOKEN)),
                (MistralAiChatOptions) createChatOptions(inputParameters));
        }
    };
}
