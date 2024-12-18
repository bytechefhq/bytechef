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

package com.bytechef.component.ai.llm.watsonx.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.URL;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.DECODING_METHOD;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.MIN_TOKENS;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.PROJECT_ID;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.REPETITION_PENALTY;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.STREAM_ENDPOINT;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.TEXT_ENDPOINT;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.watsonx.WatsonxAiChatModel;
import org.springframework.ai.watsonx.WatsonxAiChatOptions;
import org.springframework.ai.watsonx.api.WatsonxAiApi;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
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
            MESSAGES_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            RESPONSE_SCHEMA_PROPERTY,
            string(DECODING_METHOD)
                .label("Decoding Method")
                .description("Decoding is the process that a model uses to choose the tokens in the generated output.")
                .exampleValue("greedy")
                .advancedOption(true),
            number(REPETITION_PENALTY)
                .label("Repetition Penalty")
                .description(
                    "Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions more strongly, while a lower value (e.g., 1.1) will be more lenient.")
                .advancedOption(true),
            integer(MIN_TOKENS)
                .label("Min Tokens")
                .description("Sets how many tokens must the LLM generate.")
                .advancedOption(true),
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY,
            SEED_PROPERTY)
        .output()
        .perform(WatsonxChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> new WatsonxAiChatModel(
        new WatsonxAiApi(
            connectionParameters.getString(URL),
            connectionParameters.getString(STREAM_ENDPOINT), connectionParameters.getString(TEXT_ENDPOINT),
            null, connectionParameters.getString(PROJECT_ID), connectionParameters.getString(TOKEN),
            RestClient.builder()),
        WatsonxAiChatOptions.builder()
            .withModel(inputParameters.getString(MODEL))
            .withTemperature(inputParameters.getDouble(TEMPERATURE))
            .withMaxNewTokens(inputParameters.getInteger(MAX_TOKENS))
            .withTopP(inputParameters.getDouble(TOP_P))
            .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
            .withTopK(inputParameters.getInteger(TOP_K))
            .withMinNewTokens(inputParameters.getInteger(MIN_TOKENS))
            .withRandomSeed(inputParameters.getInteger(SEED))
            .withRepetitionPenalty(inputParameters.getDouble(REPETITION_PENALTY))
            .withDecodingMethod(inputParameters.getString(DECODING_METHOD))
            .build());

    private WatsonxChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
