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
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.bedrock.titan.BedrockTitanChatModel;
import org.springframework.ai.bedrock.titan.BedrockTitanChatOptions;
import org.springframework.ai.bedrock.titan.api.TitanChatBedrockApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/**
 * @author Marko Kriskovic
 */
public class AmazonBedrockTitanChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("askTitan")
        .title("Ask Titan")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(AmazonBedrockConstants.TITAN_MODELS),
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            STOP_PROPERTY)
        .output(LLMUtils::output)
        .perform(AmazonBedrockTitanChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> new BedrockTitanChatModel(
        new TitanChatBedrockApi(inputParameters.getRequiredString(MODEL),
            () -> AwsBasicCredentials.create(
                connectionParameters.getRequiredString(ACCESS_KEY_ID),
                connectionParameters.getRequiredString(SECRET_ACCESS_KEY)),
            connectionParameters.getRequiredString(REGION), new ObjectMapper()),
        BedrockTitanChatOptions.builder()
            .withTemperature(inputParameters.getDouble(TEMPERATURE))
            .withMaxTokenCount(inputParameters.getInteger(MAX_TOKENS))
            .withTopP(inputParameters.getDouble(TOP_P))
            .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .build());

    private AmazonBedrockTitanChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
