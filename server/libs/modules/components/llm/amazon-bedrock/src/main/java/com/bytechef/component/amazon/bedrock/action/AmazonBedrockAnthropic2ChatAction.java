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

package com.bytechef.component.amazon.bedrock.action;

import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.ACCESS_KEY_ID;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_SCHEMA_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.llm.util.LLMUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.bedrock.anthropic.AnthropicChatOptions;
import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;
import org.springframework.ai.bedrock.anthropic.api.AnthropicChatBedrockApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/**
 * @author Marko Kriskovic
 */
public class AmazonBedrockAnthropic2ChatAction {

    public static final List<Option<String>> MODELS_ENUM = LLMUtils
        .getEnumOptions(Arrays.stream(AnthropicChatBedrockApi.AnthropicChatModel.values())
        .collect(
            Collectors.toMap(
                AnthropicChatBedrockApi.AnthropicChatModel::getName,
                AnthropicChatBedrockApi.AnthropicChatModel::getName, (f, s) -> f)));

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("askAnthropic2")
        .title("Ask Anthropic2")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(MODELS_ENUM),
            MESSAGES_PROPERTY,
            integer(MAX_TOKENS)
                .label("Max Tokens")
                .description("The maximum number of tokens to generate in the chat completion.")
                .required(true),
            RESPONSE_FORMAT_PROPERTY,
            RESPONSE_SCHEMA_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY)
        .output()
        .perform(AmazonBedrockAnthropic2ChatAction::perform);

    private AmazonBedrockAnthropic2ChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return CHAT.getResponse(inputParameters, connectionParameters, context);
    }

    public static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return AnthropicChatOptions.builder()
                .withTemperature(inputParameters.getDouble(TEMPERATURE))
                .withMaxTokensToSample(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getDouble(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new BedrockAnthropicChatModel(
                new AnthropicChatBedrockApi(
                    inputParameters.getRequiredString(MODEL),
                    () -> AwsBasicCredentials.create(
                        connectionParameters.getRequiredString(ACCESS_KEY_ID),
                        connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
                    connectionParameters.getRequiredString(REGION), new ObjectMapper()),
                (AnthropicChatOptions) createChatOptions(inputParameters));
        }
    };
}
