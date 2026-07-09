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

package com.bytechef.component.ai.llm.openai.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_COMPLETION_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STORE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.openai.models.ChatModel;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.speech.SpeechModel;
import com.openai.models.embeddings.EmbeddingModel;
import com.openai.models.images.ImageModel;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class OpenAiConstants {

    public static final String QUALITY = "quality";

    public static final List<Option<String>> IMAGE_MODELS = ModelUtils.getEnumOptions(
        toModelMap(Arrays.stream(ImageModel.Known.values())
            .map(ImageModel.Known::name)));

    public static final List<Option<String>> CHAT_MODELS = ModelUtils.getEnumOptions(toChatModelMap());

    public static final List<Option<String>> EMBEDDING_MODELS = ModelUtils.getEnumOptions(
        toModelMap(Arrays.stream(EmbeddingModel.Known.values())
            .map(EmbeddingModel.Known::name)));

    public static final List<Option<String>> SPEECH_MODELS = ModelUtils.getEnumOptions(
        toModelMap(Arrays.stream(SpeechModel.Known.values())
            .map(SpeechModel.Known::name)));

    public static final List<Option<String>> TRANSCRIPTION_MODELS = ModelUtils.getEnumOptions(
        toModelMap(Arrays.stream(AudioModel.Known.values())
            .map(AudioModel.Known::name)));

    public static final Property CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true)
        .options(CHAT_MODELS);

    public static final List<Property> ASK_PROPERTIES = List.of(
        CHAT_MODEL_PROPERTY,
        FORMAT_PROPERTY,
        PROMPT_PROPERTY,
        SYSTEM_PROMPT_PROPERTY,
        ATTACHMENTS_PROPERTY,
        MESSAGES_PROPERTY,
        RESPONSE_PROPERTY,
        MAX_TOKENS_PROPERTY,
        MAX_COMPLETION_TOKENS_PROPERTY,
        N_PROPERTY,
        TEMPERATURE_PROPERTY,
        TOP_P_PROPERTY,
        FREQUENCY_PENALTY_PROPERTY,
        PRESENCE_PENALTY_PROPERTY,
        LOGIT_BIAS_PROPERTY,
        STOP_PROPERTY,
        USER_PROPERTY,
        REASONING_PROPERTY,
        VERBOSITY_PROPERTY,
        STORE_PROPERTY);

    private OpenAiConstants() {
    }

    private static Map<String, String> toModelMap(Stream<String> knownNames) {
        return knownNames
            .map(name -> name.toLowerCase()
                .replace('_', '-'))
            .collect(Collectors.toMap(Function.identity(), Function.identity(), (a, b) -> a));
    }

    /**
     * Chat model ids contain version dots (e.g. {@code gpt-3.5-turbo}) that the enum-name to '-' transform corrupts, so
     * read the real wire value straight off the SDK's {@link ChatModel} constants via {@link ChatModel#asString()}.
     */
    private static Map<String, String> toChatModelMap() {
        return Arrays.stream(ChatModel.class.getFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == ChatModel.class)
            .map(OpenAiConstants::resolveChatModelValue)
            .collect(Collectors.toMap(Function.identity(), Function.identity(), (a, b) -> a));
    }

    private static String resolveChatModelValue(Field field) {
        try {
            return ((ChatModel) field.get(null)).asString();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
