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

package com.bytechef.component.ai.llm.constant;

/**
 * @author Ivica Cardic
 */
public enum Provider {

    AMAZON_BEDROCK_ANTHROPIC2(
        1, "amazonBedrockAnthropic2", "ai.provider.amazonBedrockAnthropic2", "Amazon Bedrock: Anthropic 2"),
    AMAZON_BEDROCK_ANTHROPIC3(
        2, "amazonBedrockAnthropic3", "ai.provider.amazonBedrockAnthropic3", "Amazon Bedrock: Anthropic 3"),
    AMAZON_BEDROCK_COHERE(3, "amazonBedrockCohere", "ai.provider.amazonBedrockCohere", "Amazon Bedrock: Cohere"),
    AMAZON_BEDROCK_JURASSIC2(
        4, "amazonBedrockJurassic2", "ai.provider.amazonBedrockJurassic2", "Amazon Bedrock: Jurassic 2"),
    AMAZON_BEDROCK_LLAMA(5, "amazonBedrockLlama", "ai.provider.amazonBedrockLlama", "Amazon Bedrock: Llama"),
    AMAZON_BEDROCK_TITAN(6, "amazonBedrockTitan", "ai.provider.amazonBedrockTitan", "Amazon Bedrock: Titan"),
    ANTHROPIC(7, "anthropic", "ai.provider.anthropic", "Anthropic"),
    AZURE_OPEN_AI(8, "azureOpenAi", "ai.provider.azureOpenAi", "Azure Open AI"),
    GROQ(9, "groq", "ai.provider.groq", "Groq"),
    HUGGING_FACE(10, "huggingFace", "ai.provider.huggingFace", "Hugging Face"),
    MISTRAL(11, "mistral", "ai.provider.mistral", "Mistral"),
    NVIDIA(12, "nvidia", "ai.provider.nvidia", "NVIDIA"),
    OPEN_AI(13, "openAi", "ai.provider.openAi", "Open AI"),
    VERTEX_GEMINI(14, "vertexGemini", "ai.provider.vertexGemini", "Vertex Gemini");

    private final int id;
    private final String label;
    private final String key;
    private final String name;

    Provider(int id, String name, String key, String label) {
        this.id = id;
        this.label = label;
        this.key = key;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
