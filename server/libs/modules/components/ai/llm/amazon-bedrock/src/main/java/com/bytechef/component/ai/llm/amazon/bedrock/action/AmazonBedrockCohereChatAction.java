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
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.BIAS_TOKEN;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.BIAS_VALUE;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.RETURN_LIKELIHOODS;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.TRUNCATE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N_PROPERTY;
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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi.CohereChatRequest.LogitBias;
import static org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi.CohereChatRequest.ReturnLikelihoods;
import static org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi.CohereChatRequest.Truncate;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.bedrock.cohere.BedrockCohereChatModel;
import org.springframework.ai.bedrock.cohere.BedrockCohereChatOptions;
import org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi;
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
                .options(AmazonBedrockConstants.COHERE_MODELS),
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
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
            string(RETURN_LIKELIHOODS)
                .label("Return Likelihoods")
                .description("The token likelihoods are returned with the response.")
                .advancedOption(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(ReturnLikelihoods.values())
                            .collect(Collectors.toMap(Enum::name, Enum::name)))),
            string(TRUNCATE)
                .label("Truncate")
                .description("Specifies how the API handles inputs longer than the maximum token length")
                .advancedOption(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(Truncate.values())
                            .collect(Collectors.toMap(Enum::name, Enum::name)))))
        .output(LLMUtils::output)
        .perform(AmazonBedrockCohereChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> new BedrockCohereChatModel(
        new CohereChatBedrockApi(
            inputParameters.getRequiredString(MODEL),
            () -> AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
            connectionParameters.getRequiredString(REGION), new ObjectMapper()),
        BedrockCohereChatOptions.builder()
            .temperature(inputParameters.getDouble(TEMPERATURE))
            .maxTokens(inputParameters.getInteger(MAX_TOKENS))
            .topP(inputParameters.getDouble(TOP_P))
            .stopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .topK(inputParameters.getInteger(TOP_K))
            .logitBias(
                new LogitBias(
                    inputParameters.getString(BIAS_TOKEN), inputParameters.getFloat(BIAS_VALUE)))
            .numGenerations(inputParameters.getInteger(N))
            .returnLikelihoods(ReturnLikelihoods.valueOf(inputParameters.getString(RETURN_LIKELIHOODS)))
            .truncate(Truncate.valueOf(inputParameters.getString(TRUNCATE)))
            .build());

    private AmazonBedrockCohereChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
