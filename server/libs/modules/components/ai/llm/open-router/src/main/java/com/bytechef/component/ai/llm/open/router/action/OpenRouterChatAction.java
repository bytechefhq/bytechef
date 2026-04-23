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
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGPROBS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_COMPLETION_TOKENS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_COMPLETION_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_LOGPROBS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.VERBOSITY_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.ai.llm.open.router.model.OpenRouterChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            SUPPORTED_PARAMETERS_PROPERTY,
            CHAT_MODEL_PROPERTY,
            PROMPT_PROPERTY,
            FORMAT_PROPERTY,
            SYSTEM_PROMPT_PROPERTY,
            ATTACHMENTS_PROPERTY,
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            LOGPROBS_PROPERTY,
            MAX_COMPLETION_TOKENS_PROPERTY,
            MAX_TOKENS_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            REASONING_PROPERTY,
            SEED_PROPERTY,
            STOP_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_LOGPROBS_PROPERTY,
            TOP_K_PROPERTY,
            TOP_P_PROPERTY,
            VERBOSITY_PROPERTY,
            USER_PROPERTY)
        .output(ModelUtils::output)
        .perform(OpenRouterChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        boolean jsonFormat = false;

        if (responseFormatRequired) {
            ResponseFormat responseFormat = inputParameters.getRequiredFromPath(
                RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class);

            jsonFormat = !responseFormat.equals(TEXT);
        }

        return OpenRouterChatModel.builder()
            .apiKey(connectionParameters.getString(TOKEN))
            .model(inputParameters.getRequiredString(MODEL))
            .frequencyPenalty(inputParameters.getDouble(FREQUENCY_PENALTY))
            .logitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}))
            .logprobs(inputParameters.getBoolean(LOGPROBS))
            .maxCompletionTokens(inputParameters.getInteger(MAX_COMPLETION_TOKENS))
            .maxTokens(inputParameters.getInteger(MAX_TOKENS))
            .presencePenalty(inputParameters.getDouble(PRESENCE_PENALTY))
            .reasoning(inputParameters.getString(REASONING))
            .jsonResponseFormat(jsonFormat)
            .seed(inputParameters.getInteger(SEED))
            .stop(inputParameters.getList(STOP, new TypeReference<>() {}))
            .temperature(inputParameters.getDouble(TEMPERATURE))
            .topK(inputParameters.getDouble(TOP_K))
            .topLogprobs(inputParameters.getInteger(TOP_LOGPROBS))
            .topP(inputParameters.getDouble(TOP_P))
            .verbosity(inputParameters.getString(VERBOSITY))
            .user(inputParameters.getString(USER))
            .build();
    };

    private OpenRouterChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
