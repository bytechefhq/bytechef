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

package com.bytechef.component.ai.llm.openai.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.openai.action.OpenAiChatAction.CHAT_MODEL;
import static com.bytechef.component.ai.llm.openai.constant.OpenAiConstants.CHAT_MODEL_PROPERTY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
public class OpenAiChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("OpenAI Model")
            .description("OpenAI model.")
            .type(ModelFunction.MODEL)
            .object(() -> OpenAiChatModel::apply)
            .properties(
                CHAT_MODEL_PROPERTY,
                MAX_TOKENS_PROPERTY,
                N_PROPERTY,
                TEMPERATURE_PROPERTY,
                TOP_P_PROPERTY,
                FREQUENCY_PENALTY_PROPERTY,
                PRESENCE_PENALTY_PROPERTY,
                LOGIT_BIAS_PROPERTY,
                STOP_PROPERTY,
                USER_PROPERTY);

    protected static ChatModel apply(
        Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired) {

        return CHAT_MODEL.createChatModel(inputParameters, connectionParameters, responseFormatRequired);
    }
}
