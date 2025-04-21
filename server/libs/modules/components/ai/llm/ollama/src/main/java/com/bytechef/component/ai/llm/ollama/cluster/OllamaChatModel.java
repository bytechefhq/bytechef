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

package com.bytechef.component.ai.llm.ollama.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.F_16_KV_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.KEEP_ALIVE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOGITS_ALL_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOW_VRAM_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAIN_GPU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_ETA_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_TAU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUL_KEEP_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_BATCH_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_CTX_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_GPU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_THREAD_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.PENALIZE_NEW_LINE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_LAST_N_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TFS_Z_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TRUNCATE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TYPICAL_P_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_M_LOCK_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_M_MAP_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_NUMA_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.VOCAB_ONLY_PROPERTY;

import com.bytechef.component.ai.llm.ollama.action.OllamaChatAction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Monika Kušter
 */
public class OllamaChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("Ollama Model")
            .description("Ollama model.")
            .type(ModelFunction.MODEL)
            .object(() -> OllamaChatModel::apply)
            .properties(
                CHAT_MODEL_PROPERTY,
                KEEP_ALIVE_PROPERTY,
                MAX_TOKENS_PROPERTY,
                TEMPERATURE_PROPERTY,
                TOP_P_PROPERTY,
                TOP_K_PROPERTY,
                FREQUENCY_PENALTY_PROPERTY,
                PRESENCE_PENALTY_PROPERTY,
                STOP_PROPERTY,
                SEED_PROPERTY,
                USE_NUMA_PROPERTY,
                NUM_CTX_PROPERTY,
                NUM_BATCH_PROPERTY,
                NUM_GPU_PROPERTY,
                MAIN_GPU_PROPERTY,
                LOW_VRAM_PROPERTY,
                F_16_KV_PROPERTY,
                LOGITS_ALL_PROPERTY,
                VOCAB_ONLY_PROPERTY,
                USE_M_MAP_PROPERTY,
                USE_M_LOCK_PROPERTY,
                NUM_THREAD_PROPERTY,
                NUL_KEEP_PROPERTY,
                TFS_Z_PROPERTY,
                TYPICAL_P_PROPERTY,
                REPEAT_LAST_N_PROPERTY,
                REPEAT_PENALTY_PROPERTY,
                MIROSTAT_PROPERTY,
                MIROSTAT_TAU_PROPERTY,
                MIROSTAT_ETA_PROPERTY,
                PENALIZE_NEW_LINE_PROPERTY,
                TRUNCATE_PROPERTY);

    protected static ChatModel apply(Parameters inputParameters, Parameters connectionParameters) {
        return OllamaChatAction.CHAT_MODEL.createChatModel(inputParameters, connectionParameters);
    }
}
