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
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.BIAS_TOKEN;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.BIAS_VALUE;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.RETURN_LIKELIHOODS;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.TRUNCATE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.N;
import static com.bytechef.component.llm.constant.LLMConstants.N_PROPERTY;
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
import org.springframework.ai.bedrock.cohere.BedrockCohereChatModel;
import org.springframework.ai.bedrock.cohere.BedrockCohereChatOptions;
import org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/**
 * @author Marko Kriskovic
 */
public class AmazonBedrockCohereChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("askCohere")
        .title("Ask Cohere")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(
                            CohereChatBedrockApi.CohereChatModel.values())
                            .collect(
                                Collectors.toMap(
                                    CohereChatBedrockApi.CohereChatModel::getName,
                                    CohereChatBedrockApi.CohereChatModel::getName, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY,
            object(LOGIT_BIAS)
                .label("Logit Bias")
                .description("Modify the likelihood of a specified token appearing in the completion.")
                .properties(
                    string(BIAS_TOKEN)
                        .label("Token"),
                    number(BIAS_VALUE)
                        .label("Logic Bias"))
                .advancedOption(true),
            object(RETURN_LIKELIHOODS)
                .label("Return Likelihoods")
                .description("The token likelihoods are returned with the response.")
                .advancedOption(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(
                            CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods.values())
                            .collect(
                                Collectors.toMap(
                                    CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods::name, clazz -> clazz,
                                    (f, s) -> f)))),
            object(TRUNCATE)
                .label("Truncate")
                .description("Specifies how the API handles inputs longer than the maximum token length")
                .advancedOption(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(CohereChatBedrockApi.CohereChatRequest.Truncate.values())
                            .collect(
                                Collectors.toMap(
                                    CohereChatBedrockApi.CohereChatRequest.Truncate::name, clazz -> clazz,
                                    (f, s) -> f)))))
        .output()
        .perform(AmazonBedrockCohereChatAction::perform);

    private AmazonBedrockCohereChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return BedrockCohereChatOptions.builder()
                .withTemperature(inputParameters.getDouble(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getDouble(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K))
                .withLogitBias(new CohereChatBedrockApi.CohereChatRequest.LogitBias(
                    inputParameters.getString(BIAS_TOKEN), inputParameters.getFloat(BIAS_VALUE)))
                .withNumGenerations(inputParameters.getInteger(N))
                .withReturnLikelihoods(inputParameters.get(RETURN_LIKELIHOODS,
                    CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods.class))
                .withTruncate(inputParameters.get(TRUNCATE,
                    CohereChatBedrockApi.CohereChatRequest.Truncate.class))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new BedrockCohereChatModel(
                new CohereChatBedrockApi(
                    inputParameters.getRequiredString(MODEL),
                    () -> AwsBasicCredentials.create(
                        connectionParameters.getRequiredString(ACCESS_KEY_ID),
                        connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
                    connectionParameters.getRequiredString(REGION), new ObjectMapper()),
                (BedrockCohereChatOptions) createChatOptions(inputParameters));
        }
    };
}
