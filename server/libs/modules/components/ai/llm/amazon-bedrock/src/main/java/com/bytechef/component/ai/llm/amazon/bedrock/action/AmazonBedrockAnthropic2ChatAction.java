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
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
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
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.bedrock.anthropic.AnthropicChatOptions;
import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;
import org.springframework.ai.bedrock.anthropic.api.AnthropicChatBedrockApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/**
 * @author Marko Kriskovic
 */
public class AmazonBedrockAnthropic2ChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("askAnthropic2")
        .title("Ask Anthropic2")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(AmazonBedrockConstants.ANTHROPIC2_MODELS),
            MESSAGES_PROPERTY,
            integer(MAX_TOKENS)
                .label("Max Tokens")
                .description("The maximum number of tokens to generate in the chat completion.")
                .required(true),
            RESPONSE_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY)
        .output(LLMUtils::output)
        .perform(AmazonBedrockAnthropic2ChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> new BedrockAnthropicChatModel(
        new AnthropicChatBedrockApi(
            inputParameters.getRequiredString(MODEL),
            () -> AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
            connectionParameters.getRequiredString(REGION), new ObjectMapper()),
        AnthropicChatOptions.builder()
            .temperature(inputParameters.getDouble(TEMPERATURE))
            .maxTokensToSample(inputParameters.getInteger(MAX_TOKENS))
            .topP(inputParameters.getDouble(TOP_P))
            .stopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .topK(inputParameters.getInteger(TOP_K))
            .build());

    private AmazonBedrockAnthropic2ChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
