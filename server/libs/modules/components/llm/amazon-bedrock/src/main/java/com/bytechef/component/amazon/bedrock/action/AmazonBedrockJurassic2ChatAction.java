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
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.COUNT_PENALTY;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.MIN_TOKENS;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.N;
import static com.bytechef.component.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.PROMPT;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatModel;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatOptions;
import org.springframework.ai.bedrock.jurassic2.api.Ai21Jurassic2ChatBedrockApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

public class AmazonBedrockJurassic2ChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("askJurassic2")
        .title("Ask Jurassic2")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel.values())
                            .collect(
                                Collectors.toMap(
                                    Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName,
                                    Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            integer(MIN_TOKENS)
                .label("Min Tokens")
                .description("The minimum number of tokens to generate in the chat completion.")
                .advancedOption(true),
            MAX_TOKENS_PROPERTY,
            string(PROMPT)
                .label("Prompt")
                .description("The text which the model is requested to continue.")
                .advancedOption(true),
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            STOP_PROPERTY,
            number(COUNT_PENALTY)
                .label("Count Penalty")
                .description("Penalty object for count.")
                .advancedOption(true))
        .output()
        .perform(AmazonBedrockJurassic2ChatAction::perform);

    private AmazonBedrockJurassic2ChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return BedrockAi21Jurassic2ChatOptions.builder()
                .withTemperature(inputParameters.getDouble(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getDouble(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K))
                .withMinTokens(inputParameters.getInteger(MIN_TOKENS))
                .withNumResults(inputParameters.getInteger(N))
                .withPrompt(inputParameters.getString(PROMPT))
                .withCountPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(COUNT_PENALTY))
                    .build())
                .withFrequencyPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(FREQUENCY_PENALTY))
                    .build())
                .withPresencePenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(PRESENCE_PENALTY))
                    .build())
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new BedrockAi21Jurassic2ChatModel(
                new Ai21Jurassic2ChatBedrockApi(
                    inputParameters.getRequiredString(MODEL),
                    () -> AwsBasicCredentials.create(
                        connectionParameters.getRequiredString(ACCESS_KEY_ID),
                        connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
                    connectionParameters.getRequiredString(REGION), new ObjectMapper()),
                (BedrockAi21Jurassic2ChatOptions) createChatOptions(inputParameters));
        }
    };
}
