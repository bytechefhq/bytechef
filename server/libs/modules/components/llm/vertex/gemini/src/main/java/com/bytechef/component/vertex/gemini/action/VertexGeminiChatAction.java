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

package com.bytechef.component.vertex.gemini.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.ASK;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.N;
import static com.bytechef.component.llm.constants.LLMConstants.STOP;
import static com.bytechef.component.llm.constants.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_K;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.LOCATION;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.PROJECT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.LLMUtils;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.google.cloud.vertexai.VertexAI;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;

public class VertexGeminiChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask Gemini")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(VertexAiGeminiChatModel.ChatModel.values())
                        .collect(Collectors.toMap(
                            VertexAiGeminiChatModel.ChatModel::getValue, VertexAiGeminiChatModel.ChatModel::getValue,
                            (f, s) -> f)))),
            MESSAGE_PROPERTY,
            MAX_TOKENS_PROPERTY,
            integer(N)
                .label("Candidate Count")
                .description(
                    "The number of generated response messages to return. This value must be between [1, 8], inclusive. Defaults to 1.")
                .minValue(0)
                .maxValue(8)
                .advancedOption(true),
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            STOP_PROPERTY)
        .outputSchema(string())
        .perform(VertexGeminiChatAction::perform);

    private VertexGeminiChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return VertexAiGeminiChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxOutputTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStopSequences(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getFloat(TOP_K))
                .withCandidateCount(inputParameters.getInteger(N))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new VertexAiGeminiChatModel(
                new VertexAI(connectionParameters.getString(PROJECT_ID), connectionParameters.getString(LOCATION)),
                (VertexAiGeminiChatOptions) createChatOptions(inputParameters));
        }
    };
}
