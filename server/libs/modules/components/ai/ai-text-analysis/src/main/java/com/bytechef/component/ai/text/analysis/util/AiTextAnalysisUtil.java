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

package com.bytechef.component.ai.text.analysis.util;

import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER;

import com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants;
import com.bytechef.component.ai.llm.mistral.constant.MistralConstants;
import com.bytechef.component.ai.llm.vertex.gemini.constant.VertexGeminiConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.constant.OpenAiConstants;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class AiTextAnalysisUtil {

    private AiTextAnalysisUtil() {
    }

    public static List<? extends Option<String>> createModelProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Integer modelProvider = inputParameters.getInteger(MODEL_PROVIDER);

        return switch (modelProvider) {
            case 0 -> AmazonBedrockConstants.ANTHROPIC2_MODELS;
            case 1 -> AmazonBedrockConstants.ANTHROPIC3_MODELS;
            case 2 -> AmazonBedrockConstants.COHERE_MODELS;
            case 3 -> AmazonBedrockConstants.JURASSIC2_MODELS;
            case 4 -> AmazonBedrockConstants.LLAMA_MODELS;
            case 5 -> AmazonBedrockConstants.TITAN_MODELS;
            case 6 -> AnthropicConstants.MODELS;
            case 11 -> MistralConstants.MODELS;
            case 12 -> OpenAiConstants.MODELS;
            case 13 -> VertexGeminiConstants.MODELS;
            default -> throw new IllegalStateException("Unexpected value: " + modelProvider);
        };
    }

    public record Criteria(String criterion, double lowestScore, double highestScore, boolean isDecimal) {
    }
}
