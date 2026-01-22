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

package com.bytechef.component.ai.llm.mistral.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.mistralai.api.MistralAiApi;

/**
 * @author Monika Kušter
 */
public final class MistralConstants {

    public static final String FILE = "file";
    public static final String FILE_ID = "file_id";
    public static final String PURPOSE = "purpose";
    public static final String SAFE_PROMPT = "safePrompt";
    public static final String TYPE = "type";
    public static final String URL = "url";

    public static final List<Option<String>> CHAT_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(MistralAiApi.ChatModel.values())
            .filter(MistralConstants::isNotDeprecated)
            .collect(Collectors.toMap(MistralAiApi.ChatModel::getValue, MistralAiApi.ChatModel::getValue)));

    public static final List<Option<String>> EMBEDDING_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(MistralAiApi.EmbeddingModel.values())
            .collect(Collectors.toMap(MistralAiApi.EmbeddingModel::getValue, MistralAiApi.EmbeddingModel::getValue)));

    public static final Property CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true)
        .options(CHAT_MODELS);

    public static final ModifiableBooleanProperty SAFE_PROMPT_PROPERTY = bool(SAFE_PROMPT)
        .label("Safe prompt")
        .description("Should the prompt be safe for work?")
        .defaultValue(true)
        .advancedOption(true);

    private MistralConstants() {
    }

    private static boolean isNotDeprecated(Enum<?> enumConstant) {
        try {
            return enumConstant.getDeclaringClass()
                .getField(enumConstant.name())
                .getAnnotation(Deprecated.class) == null;
        } catch (NoSuchFieldException exception) {
            return true;
        }
    }
}
