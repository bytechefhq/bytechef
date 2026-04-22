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

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.action.OpenRouterChatAction.CHAT_MODEL;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_COMPLETION_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_LOGPROBS_PROPERTY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

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
                SUPPORTED_PARAMETERS_PROPERTY,
                CHAT_MODEL_PROPERTY,
                FREQUENCY_PENALTY_PROPERTY
                    .displayCondition("contains(%s, 'frequency_penalty')".formatted(SUPPORTED_PARAMETERS)),
                LOGIT_BIAS_PROPERTY
                    .displayCondition("contains(%s, 'logit_bias')".formatted(SUPPORTED_PARAMETERS)),
                LOGPROBS_PROPERTY
                    .displayCondition("contains(%s, 'logprobs')".formatted(SUPPORTED_PARAMETERS)),
                MAX_COMPLETION_TOKENS_PROPERTY
                    .displayCondition("contains(%s, 'max_completion_tokens')".formatted(SUPPORTED_PARAMETERS)),
                MAX_TOKENS_PROPERTY
                    .displayCondition("contains(%s, 'max_tokens')".formatted(SUPPORTED_PARAMETERS)),
                PRESENCE_PENALTY_PROPERTY
                    .displayCondition("contains(%s, 'presence_penalty')".formatted(SUPPORTED_PARAMETERS)),
                REASONING_PROPERTY
                    .displayCondition("contains(%s, 'reasoning')".formatted(SUPPORTED_PARAMETERS)),
                SEED_PROPERTY
                    .displayCondition("contains(%s, 'seed')".formatted(SUPPORTED_PARAMETERS)),
                STOP_PROPERTY
                    .displayCondition("contains(%s, 'stop')".formatted(SUPPORTED_PARAMETERS)),
                TEMPERATURE_PROPERTY
                    .displayCondition("contains(%s, 'temperature')".formatted(SUPPORTED_PARAMETERS)),
                TOP_LOGPROBS_PROPERTY
                    .displayCondition("contains(%s, 'top_logprobs')".formatted(SUPPORTED_PARAMETERS)),
                TOP_K_PROPERTY
                    .displayCondition("contains(%s, 'top_k')".formatted(SUPPORTED_PARAMETERS)),
                TOP_P_PROPERTY
                    .displayCondition("contains(%s, 'top_p')".formatted(SUPPORTED_PARAMETERS)),
                VERBOSITY_PROPERTY
                    .displayCondition("contains(%s, 'verbosity')".formatted(SUPPORTED_PARAMETERS)),
                USER_PROPERTY);

    protected static ChatModel apply(
        Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired) {

        return CHAT_MODEL.createChatModel(inputParameters, connectionParameters, responseFormatRequired);
    }
}
