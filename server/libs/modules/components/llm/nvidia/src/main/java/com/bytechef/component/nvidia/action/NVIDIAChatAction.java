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

package com.bytechef.component.nvidia.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.ASK;
import static com.bytechef.component.llm.constants.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.llm.constants.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.llm.constants.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.N;
import static com.bytechef.component.llm.constants.LLMConstants.N_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.llm.constants.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.STOP;
import static com.bytechef.component.llm.constants.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.USER;
import static com.bytechef.component.llm.constants.LLMConstants.USER_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.util.interfaces.Chat;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * @author Monika Domiter
 */
public class NVIDIAChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true),
            MESSAGE_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            STOP_PROPERTY,
            USER_PROPERTY)
        .outputSchema(string())
        .perform(NVIDIAChatAction::perform);

    private NVIDIAChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return OpenAiChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withFrequencyPenalty(inputParameters.getFloat(FREQUENCY_PENALTY))
                .withLogitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withN(inputParameters.getInteger(N))
                .withPresencePenalty(inputParameters.getFloat(PRESENCE_PENALTY))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withUser(inputParameters.getString(USER))
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new OpenAiChatModel(new OpenAiApi(connectionParameters.getString(TOKEN)),
                (OpenAiChatOptions) createChatOptions(inputParameters));
        }
    };
}
