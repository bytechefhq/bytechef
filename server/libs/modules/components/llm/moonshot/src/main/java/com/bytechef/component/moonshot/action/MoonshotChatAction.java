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

package com.bytechef.component.moonshot.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.ASK;
import static com.bytechef.component.llm.constants.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.llm.constants.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.FUNCTIONS;
import static com.bytechef.component.llm.constants.LLMConstants.FUNCTIONS_PROERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.N;
import static com.bytechef.component.llm.constants.LLMConstants.N_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.llm.constants.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT_PROPERTY;
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
import com.bytechef.component.llm.util.LLMUtils;
import com.bytechef.component.llm.util.interfaces.Chat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.moonshot.MoonshotChatModel;
import org.springframework.ai.moonshot.MoonshotChatOptions;
import org.springframework.ai.moonshot.api.MoonshotApi;

public class MoonshotChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(MoonshotApi.ChatModel.values())
                        .collect(Collectors.toMap(
                            MoonshotApi.ChatModel::getValue, MoonshotApi.ChatModel::getValue, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            STOP_PROPERTY,
            FUNCTIONS_PROERTY,
            USER_PROPERTY)
        .outputSchema(object())
        .perform(MoonshotChatAction::perform);

    private MoonshotChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            MoonshotChatOptions.Builder builder = MoonshotChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withN(inputParameters.getInteger(N))
                .withFrequencyPenalty(inputParameters.getFloat(FREQUENCY_PENALTY))
                .withPresencePenalty(inputParameters.getFloat(PRESENCE_PENALTY))
                .withUser(inputParameters.getString(USER));

            List<String> functions = inputParameters.getList(FUNCTIONS, new TypeReference<>() {});
            if (functions != null)
                builder.withFunctions(new HashSet<>(functions));
            return builder.build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new MoonshotChatModel(new MoonshotApi(connectionParameters.getString(TOKEN)),
                (MoonshotChatOptions) createChatOptions(inputParameters));
        }
    };
}
