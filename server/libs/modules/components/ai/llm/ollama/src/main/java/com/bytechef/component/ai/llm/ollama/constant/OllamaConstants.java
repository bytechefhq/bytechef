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

package com.bytechef.component.ai.llm.ollama.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.ollama.api.OllamaModel;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public final class OllamaConstants {

    public static final String URL = "url";
    public static final String FORMAT = "format";
    public static final String F16KV = "f16kv";
    public static final ModifiableBooleanProperty F_16_KV_PROPERTY = bool(F16KV)
        .label("F16 KV")
        .advancedOption(true);
    public static final String KEEP_ALIVE = "keepAlive";
    public static final ModifiableStringProperty KEEP_ALIVE_PROPERTY = string(KEEP_ALIVE)
        .label("Keep alive for")
        .description("Controls how long the model will stay loaded into memory following the request")
        .exampleValue("5m");
    public static final String LOGTS_ALL = "logitsAll";
    public static final ModifiableBooleanProperty LOGITS_ALL_PROPERTY = bool(LOGTS_ALL)
        .label("Logits all")
        .description(
            "Return logits for all the tokens, not just the last one. To enable completions to return logprobs, " +
                "this must be true.")
        .advancedOption(true);
    public static final String LOW_VRAM = "lowVram";
    public static final ModifiableBooleanProperty LOW_VRAM_PROPERTY = bool(LOW_VRAM)
        .label("Low VRAM")
        .advancedOption(true);
    public static final String MAIN_GPU = "mainGpu";
    public static final ModifiableIntegerProperty MAIN_GPU_PROPERTY = integer(MAIN_GPU)
        .label("Main GPU")
        .description(
            "When using multiple GPUs this option controls which GPU is used for small tensors for which the " +
                "overhead of splitting the computation across all GPUs is not worthwhile. The GPU in question will " +
                "use slightly more VRAM to store a scratch buffer for temporary results.")
        .advancedOption(true);
    public static final String MIROSTAT = "mirostat";
    public static final ModifiableIntegerProperty MIROSTAT_PROPERTY = integer(MIROSTAT)
        .label("Mirostat")
        .description(
            "Enable Mirostat sampling for controlling perplexity. " +
                "(default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0)")
        .advancedOption(true);
    public static final String MIROSTAT_ETA = "mirostatEta";
    public static final ModifiableNumberProperty MIROSTAT_ETA_PROPERTY = number(MIROSTAT_ETA)
        .label("Mirostat Eta")
        .description(
            "Influences how quickly the algorithm responds to feedback from the generated text. A lower learning " +
                "rate will result in slower adjustments, while a higher learning rate will make the algorithm more " +
                "responsive.")
        .advancedOption(true);
    public static final String MIROSTAT_TAU = "mirostatTau";
    public static final ModifiableNumberProperty MIROSTAT_TAU_PROPERTY = number(MIROSTAT_TAU)
        .label("Mirostat Tau")
        .description(
            "Controls the balance between coherence and diversity of the output. A lower value will result in more " +
                "focused and coherent text.")
        .advancedOption(true);
    public static final String NUM_BATCH = "numBatch";
    public static final ModifiableIntegerProperty NUM_BATCH_PROPERTY = integer(NUM_BATCH)
        .label("Num batch")
        .description("Prompt processing maximum batch size.")
        .advancedOption(true);
    public static final String NUM_CTX = "numCtx";
    public static final ModifiableIntegerProperty NUM_CTX_PROPERTY = integer(NUM_CTX)
        .label("Num CTX")
        .description("Sets the size of the context window used to generate the next token.")
        .advancedOption(true);
    public static final String NUM_KEEP = "numKeep";
    public static final ModifiableIntegerProperty NUL_KEEP_PROPERTY = integer(NUM_KEEP)
        .label("Nul keep")
        .advancedOption(true);
    public static final String NUM_GPU = "numGpu";
    public static final ModifiableIntegerProperty NUM_GPU_PROPERTY = integer(NUM_GPU)
        .label("Num GPU")
        .description(
            "The number of layers to send to the GPU(s). On macOS it defaults to 1 to enable metal support, " +
                "0 to disable. 1 here indicates that NumGPU should be set dynamically")
        .advancedOption(true);
    public static final String NUM_THREAD = "numThread";
    public static final ModifiableIntegerProperty NUM_THREAD_PROPERTY = integer(NUM_THREAD)
        .label("Num thread")
        .description(
            "Sets the number of threads to use during computation. By default, Ollama will detect this for optimal " +
                "performance. It is recommended to set this value to the number of physical CPU cores your system " +
                "has (as opposed to the logical number of cores). 0 = let the runtime decide")
        .advancedOption(true);
    public static final String PENALIZE_NEW_LINE = "penalizeNewLine";
    public static final ModifiableBooleanProperty PENALIZE_NEW_LINE_PROPERTY = bool(PENALIZE_NEW_LINE)
        .label("Penalize new line")
        .advancedOption(true);
    public static final String REPEAT_LAST_N = "repeatLastN";
    public static final ModifiableIntegerProperty REPEAT_LAST_N_PROPERTY = integer(REPEAT_LAST_N)
        .label("Repeat last N")
        .description(
            "Sets how far back for the model to look back to prevent repetition. " +
                "(Default: 64, 0 = disabled, -1 = num_ctx)")
        .advancedOption(true);
    public static final String REPEAT_PENALTY = "repeatPenalty";
    public static final ModifiableNumberProperty REPEAT_PENALTY_PROPERTY = number(REPEAT_PENALTY)
        .label("Repeat penalty")
        .description(
            "Sets how strongly to penalize repetitions. A higher value (e.g., 1.5) will penalize repetitions more " +
                "strongly, while a lower value (e.g., 0.9) will be more lenient.")
        .advancedOption(true);
    public static final String TFSZ = "tfsz";
    public static final ModifiableNumberProperty TFS_Z_PROPERTY = number(TFSZ)
        .label("Tfs Z")
        .description(
            "Tail-free sampling is used to reduce the impact of less probable tokens from the output. A higher " +
                "value (e.g., 2.0) will reduce the impact more, while a value of 1.0 disables this setting.")
        .advancedOption(true);
    public static final String TRUNCATE = "truncate";
    public static final ModifiableBooleanProperty TRUNCATE_PROPERTY = bool(TRUNCATE)
        .label("Truncate")
        .advancedOption(true);
    public static final String TYPICAL_P = "typicalP";
    public static final ModifiableNumberProperty TYPICAL_P_PROPERTY = number(TYPICAL_P)
        .label("Typical P")
        .advancedOption(true);
    public static final String USE_MLOCK = "useMlock";
    public static final ModifiableBooleanProperty USE_M_LOCK_PROPERTY = bool(USE_MLOCK)
        .label("Use MLock")
        .description(
            "Lock the model in memory, preventing it from being swapped out when memory-mapped. This can improve " +
                "performance but trades away some of the advantages of memory-mapping by requiring more RAM to run " +
                "and potentially slowing down load times as the model loads into RAM.")
        .advancedOption(true);
    public static final String USE_MMAP = "useMmap";
    public static final ModifiableBooleanProperty USE_M_MAP_PROPERTY = bool(USE_MMAP)
        .label("Use MMap")
        .description(
            "By default, models are mapped into memory, which allows the system to load only the necessary parts of " +
                "the model as needed. However, if the model is larger than your total amount of RAM or if your " +
                "system is low on available memory, using mmap might increase the risk of pageouts, negatively " +
                "impacting performance. Disabling mmap results in slower load times but may reduce pageouts if " +
                "you’re not using mlock. Note that if the model is larger than the total amount of RAM, turning " +
                "off mmap would prevent the model from loading at all.")
        .advancedOption(true);
    public static final String USE_NUMA = "useNuma";
    public static final ModifiableBooleanProperty USE_NUMA_PROPERTY = bool(USE_NUMA)
        .label("Use NUMA")
        .description("Whether to use NUMA.")
        .advancedOption(true);
    public static final String VOCAB_ONLY = "vocabOnly";
    public static final ModifiableBooleanProperty VOCAB_ONLY_PROPERTY = bool(VOCAB_ONLY)
        .label("Vocab only")
        .description("Load only the vocabulary, not the weights.")
        .advancedOption(true);
    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true)
        .options(
            ModelUtils.getEnumOptions(
                Arrays.stream(OllamaModel.values())
                    .collect(Collectors.toMap(OllamaModel::getName, OllamaModel::getName))));
    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Num predict")
        .description(
            "Maximum number of tokens to predict when generating text. (-1 = infinite generation, -2 = fill context)")
        .advancedOption(true);

    private OllamaConstants() {
    }
}
