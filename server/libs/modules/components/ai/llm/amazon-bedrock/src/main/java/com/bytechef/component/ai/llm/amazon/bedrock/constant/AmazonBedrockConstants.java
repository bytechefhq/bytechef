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

package com.bytechef.component.ai.llm.amazon.bedrock.constant;

import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.bedrock.anthropic.api.AnthropicChatBedrockApi;
import org.springframework.ai.bedrock.cohere.api.CohereChatBedrockApi;
import org.springframework.ai.bedrock.jurassic2.api.Ai21Jurassic2ChatBedrockApi;
import org.springframework.ai.bedrock.llama.api.LlamaChatBedrockApi;
import org.springframework.ai.bedrock.titan.api.TitanChatBedrockApi;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class AmazonBedrockConstants {

    public static final String ACCESS_KEY_ID = "accessKey";
    public static final String BIAS_TOKEN = "biasToken";
    public static final String BIAS_VALUE = "biasValue";
    public static final String COUNT_PENALTY = "countPenalty";
    public static final String MIN_TOKENS = "truncate";
    public static final String REGION = "region";
    public static final String RETURN_LIKELIHOODS = "returnLikelihoods";
    public static final String SECRET_ACCESS_KEY = "secretKey";
    public static final String TRUNCATE = "truncate";

    public static final List<Option<String>> ANTHROPIC2_MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(AnthropicChatBedrockApi.AnthropicChatModel.values())
            .collect(
                Collectors.toMap(
                    AnthropicChatBedrockApi.AnthropicChatModel::getName,
                    AnthropicChatBedrockApi.AnthropicChatModel::getName, (f, s) -> f)));

    public static final List<Option<String>> ANTHROPIC3_MODELS = LLMUtils.getEnumOptions(
        Arrays.stream(AnthropicChatBedrockApi.AnthropicChatModel.values())
            .collect(
                Collectors.toMap(
                    AnthropicChatBedrockApi.AnthropicChatModel::getName,
                    AnthropicChatBedrockApi.AnthropicChatModel::getName, (f, s) -> f)));
    public static final List<Option<String>> COHERE_MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(CohereChatBedrockApi.CohereChatModel.values())
            .collect(
                Collectors.toMap(
                    CohereChatBedrockApi.CohereChatModel::getName,
                    CohereChatBedrockApi.CohereChatModel::getName, (f, s) -> f)));
    public static final List<Option<String>> JURASSIC2_MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel.values())
            .collect(
                Collectors.toMap(
                    Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName,
                    Ai21Jurassic2ChatBedrockApi.Ai21Jurassic2ChatModel::getName, (f, s) -> f)));
    public static final List<Option<String>> LLAMA_MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(LlamaChatBedrockApi.LlamaChatModel.values())
            .collect(
                Collectors.toMap(
                    LlamaChatBedrockApi.LlamaChatModel::getName,
                    LlamaChatBedrockApi.LlamaChatModel::getName,
                    (f, s) -> f)));
    public static final List<Option<String>> TITAN_MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(TitanChatBedrockApi.TitanChatModel.values())
            .collect(
                Collectors.toMap(
                    TitanChatBedrockApi.TitanChatModel::getName,
                    TitanChatBedrockApi.TitanChatModel::getName,
                    (f, s) -> f)));

    private AmazonBedrockConstants() {
    }
}
