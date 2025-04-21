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

package com.bytechef.component.ai.llm.vertex.gemini.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public final class VertexGeminiConstants {

    public static final String LOCATION = "location";
    public static final String PROJECT_ID = "projectId";

    public static final ModifiableIntegerProperty CANDIDATE_COUNT_PROPERTY = integer(N)
        .label("Candidate Count")
        .description(
            "The number of generated response messages to return. This value must be between [1, 8], inclusive. " +
                "Defaults to 1.")
        .minValue(0)
        .maxValue(8)
        .advancedOption(true);

    public static final List<Option<String>> MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(VertexAiGeminiChatModel.ChatModel.values())
            .collect(
                Collectors.toMap(
                    VertexAiGeminiChatModel.ChatModel::getValue, VertexAiGeminiChatModel.ChatModel::getValue)));
    public static final ComponentDsl.ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true)
        .options(MODELS);

    private VertexGeminiConstants() {
    }
}
