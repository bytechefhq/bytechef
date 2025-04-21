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

package com.bytechef.component.ai.llm.watsonx.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.DECODING_METHOD_PROPERTY;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.MIN_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.watsonx.constant.WatsonxConstants.REPETITION_PENALTY_PROPERTY;

import com.bytechef.component.ai.llm.watsonx.action.WatsonxChatAction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Monika Kušter
 */
public class WatsonxChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("Watsonx AI Model")
            .description("Watsonx AI model.")
            .type(ModelFunction.MODEL)
            .object(() -> WatsonxChatModel::apply)
            .properties(
                CHAT_MODEL_PROPERTY,
                DECODING_METHOD_PROPERTY,
                REPETITION_PENALTY_PROPERTY,
                MIN_TOKENS_PROPERTY,
                MAX_TOKENS_PROPERTY,
                TEMPERATURE_PROPERTY,
                TOP_P_PROPERTY,
                TOP_K_PROPERTY,
                STOP_PROPERTY,
                SEED_PROPERTY);

    protected static ChatModel apply(Parameters inputParameters, Parameters connectionParameters) {
        return WatsonxChatAction.CHAT_MODEL.createChatModel(inputParameters, connectionParameters);
    }
}
