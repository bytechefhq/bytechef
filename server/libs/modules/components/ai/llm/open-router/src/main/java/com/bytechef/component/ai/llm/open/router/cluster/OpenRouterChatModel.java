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

package com.bytechef.component.ai.llm.open.router.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("Open Router Model")
            .description("Open Router model.")
            .type(ModelFunction.MODEL)
            .object(() -> OpenRouterChatModel::apply)
            .properties(
                MAX_TOKENS_PROPERTY,
                TEMPERATURE_PROPERTY,
                TOP_P_PROPERTY,
                FREQUENCY_PENALTY_PROPERTY,
                PRESENCE_PENALTY_PROPERTY,
                LOGIT_BIAS_PROPERTY,
                STOP_PROPERTY,
                USER_PROPERTY);

    protected static ChatModel apply(
        Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired) {

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
                    .presencePenalty(inputParameters.getDouble(PRESENCE_PENALTY))
                    .stop(inputParameters.getList(STOP, new TypeReference<>() {}))
                    .temperature(inputParameters.getDouble(TEMPERATURE))
                    .topP(inputParameters.getDouble(TOP_P))
                    .user(inputParameters.getString(USER))
                    .build())
            .build();
    }
}
