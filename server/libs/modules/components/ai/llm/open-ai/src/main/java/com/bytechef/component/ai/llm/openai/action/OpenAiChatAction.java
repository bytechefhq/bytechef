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

package com.bytechef.component.ai.llm.openai.action;

import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.TEXT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.openai.constant.OpenAiConstants.ASK_PROPERTIES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.api.ResponseFormat.Type;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public class OpenAiChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(ASK_PROPERTIES)
        .output(ModelUtils::output)
        .perform(OpenAiChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        ResponseFormat responseFormat = null;

        if (responseFormatRequired) {
            ChatModel.ResponseFormat chatModelResponseFormat = inputParameters.getRequiredFromPath(
                RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class);

            Type type = chatModelResponseFormat.equals(TEXT) ? Type.TEXT : Type.JSON_OBJECT;

            responseFormat = ResponseFormat.builder()
                .type(type)
                .build();
        }

        return OpenAiChatModel.builder()
            .openAiApi(
                OpenAiApi.builder()
                    .apiKey(connectionParameters.getString(TOKEN))
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

    private OpenAiChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
