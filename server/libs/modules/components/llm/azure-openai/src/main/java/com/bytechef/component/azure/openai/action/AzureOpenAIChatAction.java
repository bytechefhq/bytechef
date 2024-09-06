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

package com.bytechef.component.azure.openai.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS;
import static com.bytechef.component.llm.constant.LLMConstants.FUNCTIONS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.N;
import static com.bytechef.component.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.USER;
import static com.bytechef.component.llm.constant.LLMConstants.USER_PROPERTY;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Chat;
import java.util.HashSet;
import java.util.List;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiResponseFormat;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * @author Marko Kriskovic
 */
public class AzureOpenAIChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("Deployment name, written in string.")
                .exampleValue("gpt-4o")
                .required(true),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            TOP_P_PROPERTY,
            STOP_PROPERTY,
            FUNCTIONS_PROPERTY,
            USER_PROPERTY)
        .output()
        .perform(AzureOpenAIChatAction::perform);

    private AzureOpenAIChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            Integer responseInteger = inputParameters.getInteger(RESPONSE_FORMAT);
            AzureOpenAiResponseFormat format = responseInteger == null || responseInteger < 1
                ? AzureOpenAiResponseFormat.TEXT : AzureOpenAiResponseFormat.JSON;

            AzureOpenAiChatOptions.Builder builder = AzureOpenAiChatOptions.builder()
                .withDeploymentName(inputParameters.getRequiredString(MODEL))
                .withFrequencyPenalty(inputParameters.getFloat(FREQUENCY_PENALTY))
                .withLogitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withN(inputParameters.getInteger(N))
                .withPresencePenalty(inputParameters.getFloat(PRESENCE_PENALTY))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withUser(inputParameters.getString(USER))
                .withResponseFormat(format);

            List<String> functions = inputParameters.getList(FUNCTIONS, new TypeReference<>() {});

            if (functions != null) {
                builder.withFunctions(new HashSet<>(functions));
            }

            return builder.build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            OpenAIClient openAIClient = new OpenAIClientBuilder()
                .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
                .endpoint(connectionParameters.getString(ENDPOINT))
                .buildClient();

            return new AzureOpenAiChatModel(openAIClient, (AzureOpenAiChatOptions) createChatOptions(inputParameters));
        }
    };
}
