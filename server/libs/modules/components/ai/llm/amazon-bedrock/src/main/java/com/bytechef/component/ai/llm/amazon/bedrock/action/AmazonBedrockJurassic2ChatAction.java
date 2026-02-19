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

package com.bytechef.component.ai.llm.amazon.bedrock.action;

import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.COUNT_PENALTY;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.MIN_TOKENS;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatModel;
import org.springframework.ai.bedrock.jurassic2.BedrockAi21Jurassic2ChatOptions;
import org.springframework.ai.bedrock.jurassic2.api.Ai21Jurassic2ChatBedrockApi;
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
                .options(),
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
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
        .output(LLMUtils::output)
        .perform(AmazonBedrockJurassic2ChatAction::perform);

    public static final ChatModel CHAT_MODEL =
        (inputParameters, connectionParameters) -> new BedrockAi21Jurassic2ChatModel(
            new Ai21Jurassic2ChatBedrockApi(
                inputParameters.getRequiredString(MODEL),
                () -> AwsBasicCredentials.create(
                    connectionParameters.getRequiredString(ACCESS_KEY_ID),
                    connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
                connectionParameters.getRequiredString(REGION), new ObjectMapper()),
            BedrockAi21Jurassic2ChatOptions.builder()
                .temperature(inputParameters.getDouble(TEMPERATURE))
                .maxTokens(inputParameters.getInteger(MAX_TOKENS))
                .topP(inputParameters.getDouble(TOP_P))
                .stopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .topK(inputParameters.getInteger(TOP_K))
                .minTokens(inputParameters.getInteger(MIN_TOKENS))
                .numResults(inputParameters.getInteger(N))
                .prompt(inputParameters.getString(PROMPT))
                .countPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(COUNT_PENALTY))
                    .build())
                .frequencyPenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(FREQUENCY_PENALTY))
                    .build())
                .presencePenaltyOptions(BedrockAi21Jurassic2ChatOptions.Penalty.builder()
                    .scale(inputParameters.getDouble(PRESENCE_PENALTY))
                    .build())
                .build());

    private AmazonBedrockJurassic2ChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
