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

package com.bytechef.component.vertex.palm2.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.vertex.palm2.constant.VertexPaLM2Constants.CANDIDATE_COUNT;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;

import static constants.LLMConstants.ASK;
import static constants.LLMConstants.MESSAGE_PROPERTY;
import static constants.LLMConstants.N;
import static constants.LLMConstants.N_PROPERTY;
import static constants.LLMConstants.TEMPERATURE;
import static constants.LLMConstants.TEMPERATURE_PROPERTY;
import static constants.LLMConstants.TOP_P;
import static constants.LLMConstants.TOP_P_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vertexai.palm2.VertexAiPaLm2ChatModel;
import org.springframework.ai.vertexai.palm2.VertexAiPaLm2ChatOptions;
import org.springframework.ai.vertexai.palm2.api.VertexAiPaLm2Api;
import util.interfaces.Chat;

public class VertexPaLM2ChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask Gemini")
        .description("Ask anything you want.")
        .properties(
            integer(CANDIDATE_COUNT)
                .label("Candidate Count")
                .description("The number of generated response messages to return. This value must be between [1, 8], inclusive. Defaults to 1.")
                .minValue(0)
                .maxValue(8)
                .required(false),
            MESSAGE_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY)
        .outputSchema(string())
        .perform(VertexPaLM2ChatAction::perform);

    private VertexPaLM2ChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    public static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return VertexAiPaLm2ChatOptions.builder()
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withTopK(inputParameters.getInteger(N))
                .withCandidateCount(inputParameters.getInteger(CANDIDATE_COUNT))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new VertexAiPaLm2ChatModel(new VertexAiPaLm2Api(connectionParameters.getString(TOKEN)), (VertexAiPaLm2ChatOptions) createChatOptions(inputParameters));
        }
    };
}
