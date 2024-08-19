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

import com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatModel;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatOptions;
import org.springframework.ai.bedrock.jurassic2.api.Ai21Jurassic2ChatBedrockApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import util.LLMUtils;
import util.interfaces.Chat;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.FREQUENCY_PENALTY;
import static constants.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static constants.LLMConstants.MAX_TOKENS;
import static constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static constants.LLMConstants.MESSAGE_PROPERTY;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.N_PROPERTY;
import static constants.LLMConstants.PRESENCE_PENALTY;
import static constants.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static constants.LLMConstants.PROMPT;
import static constants.LLMConstants.STOP;
import static constants.LLMConstants.STOP_PROPERTY;
import static constants.LLMConstants.TEMPERATURE;
import static constants.LLMConstants.TEMPERATURE_PROPERTY;
import static constants.LLMConstants.TOP_P;
import static constants.LLMConstants.TOP_P_PROPERTY;

public class AmazonBedrockJurassic2ChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(AmazonBedrockConstants.ASK_JURASSIC2)
        .title("Ask Jurassic2")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel.values())
                        .collect(Collectors.toMap(
                            Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName, Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName, (f,s)->f)))),
            integer(AmazonBedrockConstants.MIN_TOKENS)
                .label("Min tokens")
                .description("The minimum number of tokens to generate in the chat completion.")
                .required(false),
            string(PROMPT)
                .label("Prompt")
                .description("The text which the model is requested to continue.")
                .required(false),
            number(AmazonBedrockConstants.COUNT_PENALTY)
                .label("Count penalty")
                .description("Penalty object for count.")
                .required(false),
            MESSAGE_PROPERTY,
            N_PROPERTY,
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            STOP_PROPERTY,
            TOP_P_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY)
        .outputSchema(string())
        .perform(AmazonBedrockJurassic2ChatAction::perform);

    private AmazonBedrockJurassic2ChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    public static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return BedrockAi21Jurassic2ChatOptions.builder()
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(N))
                .withMinTokens(inputParameters.getInteger(AmazonBedrockConstants.MIN_TOKENS))
                .withNumResults(inputParameters.getInteger(N))
                .withPrompt(inputParameters.getString(PROMPT))
                .withCountPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder().scale(inputParameters.getFloat(AmazonBedrockConstants.COUNT_PENALTY)).build())
                .withFrequencyPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder().scale(inputParameters.getFloat(FREQUENCY_PENALTY)).build())
                .withPresencePenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder().scale(inputParameters.getFloat(PRESENCE_PENALTY)).build())
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new BedrockAi21Jurassic2ChatModel(new Ai21Jurassic2ChatBedrockApi(inputParameters.getRequiredString(MODEL),
                EnvironmentVariableCredentialsProvider.create(), connectionParameters.getRequiredString(AmazonBedrockConstants.REGION), new ObjectMapper()),
                (BedrockAi21Jurassic2ChatOptions) createChatOptions(inputParameters));
        }
    };
}
