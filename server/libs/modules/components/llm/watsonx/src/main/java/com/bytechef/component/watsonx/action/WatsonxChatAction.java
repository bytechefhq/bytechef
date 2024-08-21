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

package com.bytechef.component.watsonx.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.string;

import static com.bytechef.component.watsonx.constant.WatsonxConstants.DECODING_METHOD;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.MIN_TOKENS;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.PROJECT_ID;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.REPETITION_PENALTY;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.STREAM_ENDPOINT;
import static com.bytechef.component.watsonx.constant.WatsonxConstants.TEXT_ENDPOINT;
import static constants.LLMConstants.ASK;
import static constants.LLMConstants.MAX_TOKENS;
import static constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static constants.LLMConstants.MESSAGE_PROPERTY;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.SEED;
import static constants.LLMConstants.SEED_PROPERTY;
import static constants.LLMConstants.STOP;
import static constants.LLMConstants.STOP_PROPERTY;
import static constants.LLMConstants.TEMPERATURE;
import static constants.LLMConstants.TEMPERATURE_PROPERTY;
import static constants.LLMConstants.TOP_K;
import static constants.LLMConstants.TOP_K_PROPERTY;
import static constants.LLMConstants.TOP_P;
import static constants.LLMConstants.TOP_P_PROPERTY;
import static constants.LLMConstants.URL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.watsonx.WatsonxAiChatModel;
import org.springframework.ai.watsonx.WatsonxAiChatOptions;
import org.springframework.ai.watsonx.api.WatsonxAiApi;
import org.springframework.web.client.RestClient;
import util.interfaces.Chat;

public class WatsonxChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("Model is the identifier of the LLM Model to be used.")
                .exampleValue("google/flan-ul2")
                .required(false),
            MESSAGE_PROPERTY,
            string(DECODING_METHOD)
                .label("Decoding method")
                .description("Decoding is the process that a model uses to choose the tokens in the generated output.")
                .exampleValue("greedy")
                .advancedOption(true),
            number(REPETITION_PENALTY)
                .label("Repetition penalty")
                .description("Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions more strongly, while a lower value (e.g., 1.1) will be more lenient.")
                .advancedOption(true),
            integer(MIN_TOKENS)
                .label("Min tokens")
                .description("Sets how many tokens must the LLM generate.")
                .advancedOption(true),
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY,
            SEED_PROPERTY)
        .outputSchema(string())
        .perform(WatsonxChatAction::perform);

    private WatsonxChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return WatsonxAiChatOptions.builder()
                .withModel(inputParameters.getString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxNewTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(TOP_K))
                .withMinNewTokens(inputParameters.getInteger(MIN_TOKENS))
                .withRandomSeed(inputParameters.getInteger(SEED))
                .withRepetitionPenalty(inputParameters.getFloat(REPETITION_PENALTY))
                .withDecodingMethod(inputParameters.getString(DECODING_METHOD))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new WatsonxAiChatModel(new WatsonxAiApi(connectionParameters.getString(URL),
                connectionParameters.getString(STREAM_ENDPOINT), connectionParameters.getString(TEXT_ENDPOINT),
                connectionParameters.getString(PROJECT_ID), connectionParameters.getString(TOKEN), RestClient.builder()),
                (WatsonxAiChatOptions) createChatOptions(inputParameters));
        }
    };
}
