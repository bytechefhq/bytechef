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
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class OpenAiConstants {

    public static final String QUALITY = "quality";

    public static final List<Option<String>> IMAGE_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(OpenAiImageApi.ImageModel.values())
            .collect(Collectors.toMap(OpenAiImageApi.ImageModel::getValue, OpenAiImageApi.ImageModel::getValue)));

    public static final List<Option<String>> CHAT_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(OpenAiApi.ChatModel.values())
            .collect(Collectors.toMap(
                chatModel -> {
                    String value = chatModel.getValue();

                    return value.replace("\n", "");
                },
                chatModel -> {
                    String value = chatModel.getValue();

                    return value.replace("\n", "");
                })));

    public static final List<Option<String>> EMBEDDING_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(OpenAiApi.EmbeddingModel.values())
            .collect(Collectors.toMap(OpenAiApi.EmbeddingModel::getValue, OpenAiApi.EmbeddingModel::getValue)));

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
        N_PROPERTY,
        TEMPERATURE_PROPERTY,
        TOP_P_PROPERTY,
        FREQUENCY_PENALTY_PROPERTY,
        PRESENCE_PENALTY_PROPERTY,
        LOGIT_BIAS_PROPERTY,
        STOP_PROPERTY,
        USER_PROPERTY);

    private OpenAiConstants() {
    }
}
