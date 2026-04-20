/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.llm.open.router.action;

import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.TEXT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.getOpenRouterModels;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            array(SUPPORTED_PARAMETERS)
                .label("Supported parameters")
                .description("Filter models by supported parameter")
                .items(string())
                .options(getSupportedParametersOptions())
                .defaultValue("response_format")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .options(getOpenRouterModels())
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS)
                .required(true),
            PROMPT_PROPERTY,
            FORMAT_PROPERTY,
            SYSTEM_PROMPT_PROPERTY,
            ATTACHMENTS_PROPERTY,
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY
                .displayCondition("contains({'response_format', 'structured_outputs'}, %s)".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            FREQUENCY_PENALTY_PROPERTY
                .displayCondition("contains(%s, 'frequency_penalty')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            LOGIT_BIAS_PROPERTY
                .displayCondition("contains(%s, 'logit_bias')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            MAX_TOKENS_PROPERTY
                .displayCondition("contains(%s, 'max_tokens')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            PRESENCE_PENALTY_PROPERTY
                .displayCondition("contains(%s, 'presence_penalty')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            REASONING_PROPERTY
                .displayCondition("contains(%s, 'reasoning')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            SEED_PROPERTY
                .displayCondition("contains(%s, 'seed')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            STOP_PROPERTY
                .displayCondition("contains(%s, 'stop')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            TEMPERATURE_PROPERTY
                .displayCondition("contains(%s, 'temperature')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            TOP_K_PROPERTY
                .displayCondition("contains(%s, 'top_k')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            TOP_P_PROPERTY
                .displayCondition("contains(%s, 'top_p')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS),
            VERBOSITY_PROPERTY
                .displayCondition("contains(%s, 'verbosity')".formatted(SUPPORTED_PARAMETERS))
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS))
        .output(ModelUtils::output)
        .perform(OpenRouterChatAction::perform);

    private static List<Option<String>> getSupportedParametersOptions() {
        return Arrays.stream(getSupportedParametersString())
            .map(param -> option(param, param))
            .collect(Collectors.toList());
    }

    private static String[] getSupportedParametersString() {
        return new String[] {
            "frequency_penalty",
            "include_reasoning",
            "logit_bias", "logprobs",
            "max_completion_tokens", "max_tokens", "min_p",
            "parallel_tool_calls", "presence_penalty",
            "reasoning", "reasoning_effort", "response_format", "repetition_penalty",
            "seed", "stop", "structured_outputs",
            "temperature", "tools", "tool_choice", "top_p", "top_k", "top_a", "top_k", "top_logprobs",
            "verbosity",
            "web_search_options"
        };
    }

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        ResponseFormat responseFormat = null;

        if (responseFormatRequired) {
            ChatModel.ResponseFormat chatModelResponseFormat = inputParameters.getRequiredFromPath(
                RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class);

            ResponseFormat.Type type =
                chatModelResponseFormat.equals(TEXT) ? ResponseFormat.Type.TEXT : ResponseFormat.Type.JSON_OBJECT;

            responseFormat = ResponseFormat.builder()
                .type(type)
                .build();
        }

        return OpenAiChatModel.builder()
            .openAiApi(
                OpenAiApi.builder()
                    .apiKey(connectionParameters.getString(TOKEN))
                    .baseUrl("https://openrouter.ai/api")
                    .restClientBuilder(ModelUtils.getRestClientBuilder())
                    .build())
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(inputParameters.getRequiredString(MODEL))
                    .frequencyPenalty(inputParameters.getDouble(FREQUENCY_PENALTY))
                    .logitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}))
                    .maxTokens(inputParameters.getInteger(MAX_TOKENS))
                    .N(inputParameters.getInteger(N))
                    .presencePenalty(inputParameters.getDouble(PRESENCE_PENALTY))
                    .responseFormat(responseFormat)
                    .stop(inputParameters.getList(STOP, new TypeReference<>() {}))
                    .temperature(inputParameters.getDouble(TEMPERATURE))
                    .topP(inputParameters.getDouble(TOP_P))
                    .user(inputParameters.getString(USER))
                    .build())
            .build();
    };

    private OpenRouterChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
