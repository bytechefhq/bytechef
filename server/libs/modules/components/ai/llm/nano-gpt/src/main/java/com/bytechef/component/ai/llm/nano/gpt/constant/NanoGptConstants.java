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

package com.bytechef.component.ai.llm.nano.gpt.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY;
import static com.bytechef.component.ai.llm.nano.gpt.util.NanoGptUtils.getNanoGptChatModels;
import static com.bytechef.component.ai.llm.nano.gpt.util.NanoGptUtils.getNanoGptEmbeddingModels;
import static com.bytechef.component.ai.llm.nano.gpt.util.NanoGptUtils.getNanoGptImageModels;
import static com.bytechef.component.ai.llm.nano.gpt.util.NanoGptUtils.getNanoGptSpeechModels;
import static com.bytechef.component.ai.llm.nano.gpt.util.NanoGptUtils.getNanoGptTranscriptionModels;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Marko Kriskovic
 */
public class NanoGptConstants {

    public static final String LOGPROBS = "logprobs";
    public static final String MAX_COMPLETION_TOKENS = "maxCompletionTokens";
    public static final String SUPPORTED_PARAMETERS = "supportedParameters";
    public static final String TOP_LOGPROBS = "topLogprobs";
    public static final String ASPECT_RATIO = "aspectRatio";
    public static final String DIMENSION = "dimension";
    public static final String MIN_P = "minP";
    public static final String MIN_TOKENS = "minTokens";
    public static final String MIROSTAT_ETA = "mirostatEta";
    public static final String MIROSTAT_MODE = "mirostatMode";
    public static final String MIROSTAT_TAU = "mirostatTau";
    public static final String REPETITION_PENALTY = "repetitionPenalty";
    public static final String TFS = "tfs";
    public static final String TOP_A = "topA";
    public static final String TYPICAL_P = "typicalP";
    public static final String GUIDANCE_SCALE = "guidanceScale";
    public static final String N = "n";
    public static final String NUM_INFERENCE_STEPS = "numInferenceSteps";
    public static final String STRENGTH = "strength";

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getNanoGptChatModels())
        .required(true);

    public static final ModifiableStringProperty IMAGE_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getNanoGptImageModels())
        .required(true);

    public static final ModifiableStringProperty SPEECH_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getNanoGptSpeechModels())
        .required(true);

    public static final ModifiableStringProperty EMBEDDING_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getNanoGptEmbeddingModels())
        .required(true);

    public static final ModifiableStringProperty TRANSCRIPTION_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(getNanoGptTranscriptionModels())
        .required(true);

    public static final ModifiableStringProperty TRANSCRIPTION_LANGUAGE_PROPERTY = string("language")
        .label("Language")
        .description("The language of the input audio.")
        .options(
            option("Afrikaans", "af"),
            option("Amharic", "am"),
            option("Arabic", "ar"),
            option("Assamese", "as"),
            option("Azerbaijani", "az"),
            option("Bashkir", "ba"),
            option("Belarusian", "be"),
            option("Bulgarian", "bg"),
            option("Bengali", "bn"),
            option("Tibetan", "bo"),
            option("Breton", "br"),
            option("Bosnian", "bs"),
            option("Catalan", "ca"),
            option("Czech", "cs"),
            option("Welsh", "cy"),
            option("Danish", "da"),
            option("German", "de"),
            option("Greek", "el"),
            option("English", "en"),
            option("Spanish", "es"),
            option("Estonian", "et"),
            option("Basque", "eu"),
            option("Persian", "fa"),
            option("Finnish", "fi"),
            option("Faroese", "fo"),
            option("French", "fr"),
            option("Galician", "gl"),
            option("Gujarati", "gu"),
            option("Hausa", "ha"),
            option("Hawaiian", "haw"),
            option("Hebrew", "he"),
            option("Hindi", "hi"),
            option("Croatian", "hr"),
            option("Haitian Creole", "ht"),
            option("Hungarian", "hu"),
            option("Armenian", "hy"),
            option("Indonesian", "id"),
            option("Icelandic", "is"),
            option("Italian", "it"),
            option("Japanese", "ja"),
            option("Javanese", "jw"),
            option("Georgian", "ka"),
            option("Kazakh", "kk"),
            option("Khmer", "km"),
            option("Kannada", "kn"),
            option("Korean", "ko"),
            option("Latin", "la"),
            option("Luxembourgish", "lb"),
            option("Lingala", "ln"),
            option("Lao", "lo"),
            option("Lithuanian", "lt"),
            option("Latvian", "lv"),
            option("Malagasy", "mg"),
            option("Maori", "mi"),
            option("Macedonian", "mk"),
            option("Malayalam", "ml"),
            option("Mongolian", "mn"),
            option("Marathi", "mr"),
            option("Malay", "ms"),
            option("Maltese", "mt"),
            option("Myanmar", "my"),
            option("Nepali", "ne"),
            option("Dutch", "nl"),
            option("Norwegian Nynorsk", "nn"),
            option("Norwegian", "no"),
            option("Occitan", "oc"),
            option("Punjabi", "pa"),
            option("Polish", "pl"),
            option("Pashto", "ps"),
            option("Portuguese", "pt"),
            option("Romanian", "ro"),
            option("Russian", "ru"),
            option("Sanskrit", "sa"),
            option("Sindhi", "sd"),
            option("Sinhala", "si"),
            option("Slovak", "sk"),
            option("Slovenian", "sl"),
            option("Shona", "sn"),
            option("Somali", "so"),
            option("Albanian", "sq"),
            option("Serbian", "sr"),
            option("Sundanese", "su"),
            option("Swedish", "sv"),
            option("Swahili", "sw"),
            option("Tamil", "ta"),
            option("Telugu", "te"),
            option("Tajik", "tg"),
            option("Thai", "th"),
            option("Turkmen", "tk"),
            option("Tagalog", "tl"),
            option("Turkish", "tr"),
            option("Tatar", "tt"),
            option("Ukrainian", "uk"),
            option("Urdu", "ur"),
            option("Uzbek", "uz"),
            option("Vietnamese", "vi"),
            option("Yiddish", "yi"),
            option("Yoruba", "yo"),
            option("Cantonese", "yue"),
            option("Chinese", "zh"))
        .required(false);

    public static final ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .displayCondition("contains(%s, 'frequency_penalty')".formatted(SUPPORTED_PARAMETERS))
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit Bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .displayCondition("contains(%s, 'logit_bias')".formatted(SUPPORTED_PARAMETERS))
        .additionalProperties(number())
        .advancedOption(true);

    public static final ModifiableBooleanProperty LOGPROBS_PROPERTY = bool(LOGPROBS)
        .label("Logprobs")
        .description("Return log probabilities.")
        .displayCondition("contains(%s, 'logprobs')".formatted(SUPPORTED_PARAMETERS))
        .required(false);

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max Tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .displayCondition("contains(%s, 'max_tokens')".formatted(SUPPORTED_PARAMETERS))
        .advancedOption(true);

    public static final ModifiableIntegerProperty MAX_COMPLETION_TOKENS_PROPERTY = integer(MAX_COMPLETION_TOKENS)
        .label("Max Completion Tokens")
        .description("Maximum tokens in completion.")
        .displayCondition("contains(%s, 'max_completion_tokens')".formatted(SUPPORTED_PARAMETERS))
        .required(false);

    public static final ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .displayCondition("contains(%s, 'presence_penalty')".formatted(SUPPORTED_PARAMETERS))
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableStringProperty REASONING_PROPERTY = string(REASONING)
        .label("Reasoning effort")
        .description(
            "Constrains effort on reasoning. Reducing reasoning effort can result in faster responses and fewer " +
                "tokens used on reasoning in a response. For reasoning models for gpt-5 and o-series models only.")
        .options(
            option("none", "none"),
            option("minimal", "minimal"),
            option("low", "low"),
            option("medium", "medium"),
            option("high", "high"),
            option("maximal", "xhigh"))
        .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.TEXT.name()))
        .advancedOption(true)
        .displayCondition("contains(%s, 'reasoning_effort')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableIntegerProperty SEED_PROPERTY = integer(SEED)
        .label("Seed")
        .description("Keeping the same seed would output the same response.")
        .advancedOption(true)
        .displayCondition("contains(%s, 'seed')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableArrayProperty STOP_PROPERTY = array(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .items(string())
        .advancedOption(true)
        .displayCondition("contains(%s, 'stop')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will " +
                "make it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .advancedOption(true)
        .displayCondition("contains(%s, 'temperature')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableIntegerProperty TOP_K_PROPERTY = integer(TOP_K)
        .label("Top K")
        .description("Specify the number of token choices the generative uses to generate the next token.")
        .defaultValue(1)
        .advancedOption(true)
        .displayCondition("contains(%s, 'top_k')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableIntegerProperty TOP_LOGPROBS_PROPERTY = integer(TOP_LOGPROBS)
        .label("Top Logprobs")
        .description("Number of top log probabilities to return (0-20).")
        .displayCondition("contains(%s, 'top_logprobs')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .maxValue(20)
        .required(false);

    public static final ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top P")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .advancedOption(true)
        .displayCondition("contains(%s, 'top_p')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableStringProperty VERBOSITY_PROPERTY = string(VERBOSITY)
        .label("Verbosity")
        .description("Adjusts response verbosity. Lower levels yield shorter answers.")
        .options(
            option("low", "low"),
            option("medium", "medium"),
            option("high", "high"))
        .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.TEXT.name()))
        .advancedOption(true)
        .displayCondition("contains(%s, 'verbosity')".formatted(SUPPORTED_PARAMETERS));

    public static final ModifiableNumberProperty MIN_P_PROPERTY = number(MIN_P)
        .label("Min P")
        .description("Probability floor for candidate tokens (0–1). Helps prevent low-entropy loops.")
        .displayCondition("contains(%s, 'min_p')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .maxValue(1)
        .advancedOption(true);

    public static final ModifiableIntegerProperty MIN_TOKENS_PROPERTY = integer(MIN_TOKENS)
        .label("Min Tokens")
        .description("Minimum completion length before stop conditions fire.")
        .displayCondition("contains(%s, 'min_tokens')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .advancedOption(true);

    public static final ModifiableIntegerProperty MIROSTAT_MODE_PROPERTY = integer(MIROSTAT_MODE)
        .label("Mirostat Mode")
        .description("Enables Mirostat sampling. Set to 1 or 2 to activate.")
        .displayCondition("contains(%s, 'mirostat_mode')".formatted(SUPPORTED_PARAMETERS))
        .options(
            option("Disabled", 0),
            option("Mirostat 1", 1),
            option("Mirostat 2", 2))
        .advancedOption(true);

    public static final ModifiableNumberProperty MIROSTAT_TAU_PROPERTY = number(MIROSTAT_TAU)
        .label("Mirostat Tau")
        .description("Mirostat target entropy. Active when Mirostat Mode is 1 or 2.")
        .displayCondition("contains(%s, 'mirostat_mode')".formatted(SUPPORTED_PARAMETERS))
        .advancedOption(true);

    public static final ModifiableNumberProperty MIROSTAT_ETA_PROPERTY = number(MIROSTAT_ETA)
        .label("Mirostat Eta")
        .description("Mirostat learning rate. Active when Mirostat Mode is 1 or 2.")
        .displayCondition("contains(%s, 'mirostat_mode')".formatted(SUPPORTED_PARAMETERS))
        .advancedOption(true);

    public static final ModifiableNumberProperty REPETITION_PENALTY_PROPERTY = number(REPETITION_PENALTY)
        .label("Repetition Penalty")
        .description("Provider-agnostic repetition modifier. Values > 1 discourage repetition.")
        .displayCondition("contains(%s, 'repetition_penalty')".formatted(SUPPORTED_PARAMETERS))
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableNumberProperty TFS_PROPERTY = number(TFS)
        .label("TFS")
        .description("Tail free sampling (0–1). Value 1.0 disables.")
        .displayCondition("contains(%s, 'tfs')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .maxValue(1)
        .advancedOption(true);

    public static final ModifiableNumberProperty TOP_A_PROPERTY = number(TOP_A)
        .label("Top A")
        .description("Blends temperature and nucleus sampling behavior.")
        .displayCondition("contains(%s, 'top_a')".formatted(SUPPORTED_PARAMETERS))
        .advancedOption(true);

    public static final ModifiableNumberProperty TYPICAL_P_PROPERTY = number(TYPICAL_P)
        .label("Typical P")
        .description("Entropy-based nucleus sampling (0–1). Preserves tokens matching expected entropy.")
        .displayCondition("contains(%s, 'typical_p')".formatted(SUPPORTED_PARAMETERS))
        .minValue(0)
        .maxValue(1)
        .advancedOption(true);

    private NanoGptConstants() {
    }

    public static final String BASE_URL = "https://nano-gpt.com/api/v1";
    public static final String TRANSCRIBE_URL = "https://nano-gpt.com/api/transcribe";
}
