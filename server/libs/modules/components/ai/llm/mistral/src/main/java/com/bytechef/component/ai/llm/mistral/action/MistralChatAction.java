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

package com.bytechef.component.ai.llm.mistral.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.SAFE_PROMPT;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.SAFE_PROMPT_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.mistralai.api.MistralAiApi.ChatCompletionRequest.ResponseFormat;
import org.springframework.ai.retry.RetryUtils;

/**
 * @author Marko Kriskovic
 * @author Monika Kušter
 */
public class MistralChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            CHAT_MODEL_PROPERTY,
            FORMAT_PROPERTY,
            PROMPT_PROPERTY,
            SYSTEM_PROMPT_PROPERTY,
            ATTACHMENTS_PROPERTY,
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            STOP_PROPERTY,
            SEED_PROPERTY,
            SAFE_PROMPT_PROPERTY)
        .output(ModelUtils::output)
        .perform(MistralChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        ResponseFormat responseFormat = null;

        if (responseFormatRequired) {
            ChatModel.ResponseFormat chatModelResponseFormat = inputParameters.getRequiredFromPath(
                RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class);

            String type = chatModelResponseFormat.equals(ChatModel.ResponseFormat.TEXT) ? "text" : "json_object";

            responseFormat = new ResponseFormat(type);
        }

        return MistralAiChatModel.builder()
            .mistralAiApi(
                new MistralAiApi(
                    "https://api.mistral.ai",
                    connectionParameters.getString(TOKEN),
                    ModelUtils.getRestClientBuilder(),
                    RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
            .defaultOptions(
                MistralAiChatOptions.builder()
                    .model(inputParameters.getRequiredString(MODEL))
                    .temperature(inputParameters.getDouble(TEMPERATURE))
                    .maxTokens(inputParameters.getInteger(MAX_TOKENS))
                    .topP(inputParameters.getDouble(TOP_P))
                    .stop(inputParameters.getList(STOP, new TypeReference<>() {}))
                    .safePrompt(inputParameters.getBoolean(SAFE_PROMPT))
                    .randomSeed(inputParameters.getInteger(SEED))
                    .responseFormat(responseFormat)
                    .build())
            .build();
    };

    private MistralChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
