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

package com.bytechef.component.ai.llm.nano.gpt.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.action.NanoGptChatAction.CHAT_MODEL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.MAX_COMPLETION_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.SUPPORTED_PARAMETERS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TOP_LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.VERBOSITY_PROPERTY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Marko Kriskovic
 */
public class NanoGptChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("Open Router Model")
            .description("Open Router model.")
            .type(ModelFunction.MODEL)
            .object(() -> NanoGptChatModel::apply)
            .properties(
                SUPPORTED_PARAMETERS_PROPERTY,
                CHAT_MODEL_PROPERTY,
                FREQUENCY_PENALTY_PROPERTY,
                LOGIT_BIAS_PROPERTY,
                LOGPROBS_PROPERTY,
                MAX_COMPLETION_TOKENS_PROPERTY,
                MAX_TOKENS_PROPERTY,
                PRESENCE_PENALTY_PROPERTY,
                REASONING_PROPERTY,
                SEED_PROPERTY,
                STOP_PROPERTY,
                TEMPERATURE_PROPERTY,
                TOP_LOGPROBS_PROPERTY,
                TOP_K_PROPERTY,
                TOP_P_PROPERTY,
                VERBOSITY_PROPERTY,
                USER_PROPERTY);

    protected static ChatModel apply(
        Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired) {

        return CHAT_MODEL.createChatModel(inputParameters, connectionParameters, responseFormatRequired);
    }
}
