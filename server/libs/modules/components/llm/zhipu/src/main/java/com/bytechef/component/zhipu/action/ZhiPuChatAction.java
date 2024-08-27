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

package com.bytechef.component.zhipu.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.ASK;
import static com.bytechef.component.llm.constants.LLMConstants.FUNCTIONS;
import static com.bytechef.component.llm.constants.LLMConstants.FUNCTIONS_PROERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.llm.constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.STOP;
import static com.bytechef.component.llm.constants.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE;
import static com.bytechef.component.llm.constants.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P;
import static com.bytechef.component.llm.constants.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.USER;
import static com.bytechef.component.llm.constants.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.zhipu.constant.ZhiPuConstants.DO_SAMPLE;
import static com.bytechef.component.zhipu.constant.ZhiPuConstants.REQUEST_ID;

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
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;

public class ZhiPuChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(ZhiPuAiApi.ChatModel.values())
                        .collect(Collectors.toMap(
                            ZhiPuAiApi.ChatModel::getValue, ZhiPuAiApi.ChatModel::getValue, (f, s) -> f)))),
            MESSAGE_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            STOP_PROPERTY,
            FUNCTIONS_PROERTY,
            USER_PROPERTY,
            string(REQUEST_ID)
                .label("Request Id")
                .description(
                    "The parameter is passed by the client and must ensure uniqueness. It is used to distinguish the unique identifier for each request. If the client does not provide it, the platform will generate it by default.")
                .advancedOption(true),
            bool(DO_SAMPLE)
                .label("Do sample")
                .description(
                    "When do_sample is set to true, the sampling strategy is enabled. If do_sample is false, the sampling strategy parameters temperature and top_p will not take effect.")
                .advancedOption(true))
        .outputSchema(object())
        .perform(ZhiPuChatAction::perform);

    private ZhiPuChatAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    private static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            ZhiPuAiChatOptions.Builder builder = ZhiPuAiChatOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withUser(inputParameters.getString(USER))
                .withRequestId(inputParameters.getString(REQUEST_ID))
                .withDoSample(inputParameters.getBoolean(DO_SAMPLE));

            List<String> functions = inputParameters.getList(FUNCTIONS, new TypeReference<>() {});
            if (functions != null)
                builder.withFunctions(new HashSet<>(functions));
            return builder.build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            return new ZhiPuAiChatModel(new ZhiPuAiApi(connectionParameters.getString(TOKEN)),
                (ZhiPuAiChatOptions) createChatOptions(inputParameters));
        }
    };
}
