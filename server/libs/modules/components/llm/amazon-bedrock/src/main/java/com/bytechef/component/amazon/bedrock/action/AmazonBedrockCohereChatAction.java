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
import static com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.N;
import static com.bytechef.component.llm.constants.LLMConstants.N_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.STOP;
import static com.bytechef.component.llm.constants.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_K;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P_PROPERTY;

import com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.LLMUtils;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.bedrock.cohere.BedrockCohereChatModel;
import org.springframework.ai.bedrock.cohere.BedrockCohereChatOptions;
import org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * @author Marko Kriskovic
 */
public class AmazonBedrockCohereChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(AmazonBedrockConstants.ASK_COHERE)
        .title("Ask Cohere")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(CohereChatBedrockApi.CohereChatModel.values())
                        .collect(Collectors.toMap(
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
            object(AmazonBedrockConstants.RETURN_LIKELIHOODS)
                .label("Return Likelihoods")
                .description("The token likelihoods are returned with the response.")
                .advancedOption(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods.values())
                        .collect(Collectors.toMap(
                            CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods::name, clas -> clas,
                            (f, s) -> f)))),
            object(AmazonBedrockConstants.TRUNCATE)
                .label("Truncate")
                .description("Specifies how the API handles inputs longer than the maximum token length")
                .advancedOption(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(CohereChatBedrockApi.CohereChatRequest.Truncate.values())
                        .collect(Collectors.toMap(
                            CohereChatBedrockApi.CohereChatRequest.Truncate::name, clas -> clas, (f, s) -> f)))))
        .outputSchema(object())
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
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K))
                .withLogitBias(new CohereChatBedrockApi.CohereChatRequest.LogitBias(
                    inputParameters.getString(BIAS_TOKEN), inputParameters.getFloat(BIAS_VALUE)))
                .withNumGenerations(inputParameters.getInteger(N))
                .withReturnLikelihoods(inputParameters.get(AmazonBedrockConstants.RETURN_LIKELIHOODS,
                    CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods.class))
                .withTruncate(inputParameters.get(AmazonBedrockConstants.TRUNCATE,
                    CohereChatBedrockApi.CohereChatRequest.Truncate.class))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new BedrockCohereChatModel(new CohereChatBedrockApi(inputParameters.getRequiredString(MODEL),
                new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return AwsBasicCredentials.create(connectionParameters.getRequiredString(ACCESS_KEY_ID),
                            connectionParameters.getRequiredString(SECRET_ACCESS_KEY));
                    }
                },
                connectionParameters.getRequiredString(AmazonBedrockConstants.REGION), new ObjectMapper()),
                (BedrockCohereChatOptions) createChatOptions(inputParameters));
        }
    };
}
