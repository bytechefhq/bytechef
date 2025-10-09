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

package com.bytechef.component.ai.llm.ollama.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.CHAT_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.F16KV;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.FORMAT;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.F_16_KV_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.KEEP_ALIVE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.KEEP_ALIVE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOGITS_ALL_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOGTS_ALL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOW_VRAM;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOW_VRAM_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAIN_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAIN_GPU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_ETA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_ETA_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_TAU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_TAU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUL_KEEP_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_BATCH;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_BATCH_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_CTX;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_CTX_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_GPU_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_KEEP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_THREAD;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_THREAD_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.PENALIZE_NEW_LINE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.PENALIZE_NEW_LINE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_LAST_N;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_LAST_N_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_PENALTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TFSZ;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TFS_Z_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TRUNCATE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TRUNCATE_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TYPICAL_P;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TYPICAL_P_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MLOCK;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MMAP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_M_LOCK_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_M_MAP_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_NUMA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_NUMA_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.VOCAB_ONLY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.VOCAB_ONLY_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;

/**
 * @author Marko Kriskovic
 */
public class OllamaChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            CHAT_MODEL_PROPERTY,
            FORMAT_PROPERTY,
            PROMPT_PROPERTY,
            SYSTEM_PROMPT_PROPERTY,
            ATTACHMENTS_PROPERTY,
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
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
            TRUNCATE_PROPERTY)
        .output(ModelUtils::output)
        .perform(OllamaChatAction::perform);

    public static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters, responseFormatRequired) -> {
        String url = connectionParameters.getString(URL);

        OllamaApi ollamaApi = url.isEmpty() ? OllamaApi.builder()
            .build()
            : OllamaApi.builder()
                .baseUrl(url)
                .build();

        return OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(
                OllamaOptions.builder()
                    .model(inputParameters.getRequiredString(MODEL))
                    .temperature(inputParameters.getDouble(TEMPERATURE))
                    .topP(inputParameters.getDouble(TOP_P))
                    .stop(inputParameters.getList(STOP, new TypeReference<>() {}))
                    .topK(inputParameters.getInteger(TOP_K))
                    .frequencyPenalty(inputParameters.getDouble(FREQUENCY_PENALTY))
                    .presencePenalty(inputParameters.getDouble(PRESENCE_PENALTY))
                    .seed(inputParameters.getInteger(SEED))
                    .format(inputParameters.getString(FORMAT))
                    .keepAlive(inputParameters.getString(KEEP_ALIVE))
                    .f16KV(inputParameters.getBoolean(F16KV))
                    .logitsAll(inputParameters.getBoolean(LOGTS_ALL))
                    .useMMap(inputParameters.getBoolean(USE_MMAP))
                    .lowVRAM(inputParameters.getBoolean(LOW_VRAM))
                    .mainGPU(inputParameters.getInteger(MAIN_GPU))
                    .mirostat(inputParameters.getInteger(MIROSTAT))
                    .mirostatEta(inputParameters.getFloat(MIROSTAT_ETA))
                    .mirostatTau(inputParameters.getFloat(MIROSTAT_TAU))
                    .numBatch(inputParameters.getInteger(NUM_BATCH))
                    .numCtx(inputParameters.getInteger(NUM_CTX))
                    .numGPU(inputParameters.getInteger(NUM_GPU))
                    .numKeep(inputParameters.getInteger(NUM_KEEP))
                    .numThread(inputParameters.getInteger(NUM_THREAD))
                    .numPredict(inputParameters.getInteger(MAX_TOKENS))
                    .penalizeNewline(inputParameters.getBoolean(PENALIZE_NEW_LINE))
                    .repeatLastN(inputParameters.getInteger(REPEAT_LAST_N))
                    .repeatPenalty(inputParameters.getDouble(REPEAT_PENALTY))
                    .tfsZ(inputParameters.getFloat(TFSZ))
                    .truncate(inputParameters.getBoolean(TRUNCATE))
                    .typicalP(inputParameters.getFloat(TYPICAL_P))
                    .useMLock(inputParameters.getBoolean(USE_MLOCK))
                    .useNUMA(inputParameters.getBoolean(USE_NUMA))
                    .vocabOnly(inputParameters.getBoolean(VOCAB_ONLY))
                    .build())
            .build();
    };

    private OllamaChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context, true);
    }
}
