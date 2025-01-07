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

package com.bytechef.component.ai.llm.ollama.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.F16KV;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.FORMAT;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.KEEP_ALIVE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOGTS_ALL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOW_VRAM;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAIN_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_ETA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_TAU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_BATCH;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_CTX;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_KEEP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_THREAD;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.PENALIZE_NEW_LINE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_LAST_N;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_PENALTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TFSZ;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TRUNCATE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TYPICAL_P;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MLOCK;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MMAP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_NUMA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.VOCAB_ONLY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;

/**
 * @author Marko Kriskovic
 */
public class OllamaChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(OllamaModel.values())
                            .collect(Collectors.toMap(OllamaModel::getName, OllamaModel::getName)))),
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
            string(KEEP_ALIVE)
                .label("Keep alive for")
                .description("Controls how long the model will stay loaded into memory following the request")
                .exampleValue("5m"),
            integer(MAX_TOKENS)
                .label("Num predict")
                .description(
                    "Maximum number of tokens to predict when generating text. (-1 = infinite generation, -2 = fill context)")
                .advancedOption(true),
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOP_K_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            STOP_PROPERTY,
            SEED_PROPERTY,
            bool(USE_NUMA)
                .label("Use NUMA")
                .description("Whether to use NUMA.")
                .advancedOption(true),
            integer(NUM_CTX)
                .label("Num CTX")
                .description("Sets the size of the context window used to generate the next token.")
                .advancedOption(true),
            integer(NUM_BATCH)
                .label("Num batch")
                .description("Prompt processing maximum batch size.")
                .advancedOption(true),
            integer(NUM_GPU)
                .label("Num GPU")
                .description(
                    "The number of layers to send to the GPU(s). On macOS it defaults to 1 to enable metal support, 0 to disable. 1 here indicates that NumGPU should be set dynamically")
                .advancedOption(true),
            integer(MAIN_GPU)
                .label("Main GPU")
                .description(
                    "When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of splitting the computation across all GPUs is not worthwhile. The GPU in question will use slightly more VRAM to store a scratch buffer for temporary results.")
                .advancedOption(true),
            bool(LOW_VRAM)
                .label("Low VRAM")
                .advancedOption(true),
            bool(F16KV)
                .label("F16 KV")
                .advancedOption(true),
            bool(LOGTS_ALL)
                .label("Logits all")
                .description(
                    "Return logits for all the tokens, not just the last one. To enable completions to return logprobs, this must be true.")
                .advancedOption(true),
            bool(VOCAB_ONLY)
                .label("Vocab only")
                .description("Load only the vocabulary, not the weights.")
                .advancedOption(true),
            bool(USE_MMAP)
                .label("Use MMap")
                .description(
                    "By default, models are mapped into memory, which allows the system to load only the necessary parts of the model as needed. However, if the model is larger than your total amount of RAM or if your system is low on available memory, using mmap might increase the risk of pageouts, negatively impacting performance. Disabling mmap results in slower load times but may reduce pageouts if you’re not using mlock. Note that if the model is larger than the total amount of RAM, turning off mmap would prevent the model from loading at all.")
                .advancedOption(true),
            bool(USE_MLOCK)
                .label("Use MLock")
                .description(
                    "Lock the model in memory, preventing it from being swapped out when memory-mapped. This can improve performance but trades away some of the advantages of memory-mapping by requiring more RAM to run and potentially slowing down load times as the model loads into RAM.")
                .advancedOption(true),
            integer(NUM_THREAD)
                .label("Num thread")
                .description(
                    "Sets the number of threads to use during computation. By default, Ollama will detect this for optimal performance. It is recommended to set this value to the number of physical CPU cores your system has (as opposed to the logical number of cores). 0 = let the runtime decide")
                .advancedOption(true),
            integer(NUM_KEEP)
                .label("Nul keep")
                .advancedOption(true),
            number(TFSZ)
                .label("Tfs Z")
                .description(
                    "Tail-free sampling is used to reduce the impact of less probable tokens from the output. A higher value (e.g., 2.0) will reduce the impact more, while a value of 1.0 disables this setting.")
                .advancedOption(true),
            number(TYPICAL_P)
                .label("Typical P")
                .advancedOption(true),
            integer(REPEAT_LAST_N)
                .label("Repeat last N")
                .description(
                    "Sets how far back for the model to look back to prevent repetition. (Default: 64, 0 = disabled, -1 = num_ctx)")
                .advancedOption(true),
            number(REPEAT_PENALTY)
                .label("Repeat penalty")
                .description(
                    "Sets how strongly to penalize repetitions. A higher value (e.g., 1.5) will penalize repetitions more strongly, while a lower value (e.g., 0.9) will be more lenient.")
                .advancedOption(true),
            integer(MIROSTAT)
                .label("Mirostat")
                .description(
                    "Enable Mirostat sampling for controlling perplexity. (default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0)")
                .advancedOption(true),
            number(MIROSTAT_TAU)
                .label("Mirostat Tau")
                .description(
                    "Controls the balance between coherence and diversity of the output. A lower value will result in more focused and coherent text.")
                .advancedOption(true),
            number(MIROSTAT_ETA)
                .label("Mirostat Eta")
                .description(
                    "Influences how quickly the algorithm responds to feedback from the generated text. A lower learning rate will result in slower adjustments, while a higher learning rate will make the algorithm more responsive.")
                .advancedOption(true),
            bool(PENALIZE_NEW_LINE)
                .label("Penalize new line")
                .advancedOption(true),
            bool(TRUNCATE)
                .label("Truncate")
                .advancedOption(true))
        .output(LLMUtils::output)
        .perform(OllamaChatAction::perform);

    private static final ChatModel CHAT_MODEL = (inputParameters, connectionParameters) -> {
        String url = connectionParameters.getString(URL);

        OllamaApi ollamaApi = url.isEmpty() ? new OllamaApi() : new OllamaApi(url);

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
        return CHAT_MODEL.getResponse(inputParameters, connectionParameters, context);
    }
}
