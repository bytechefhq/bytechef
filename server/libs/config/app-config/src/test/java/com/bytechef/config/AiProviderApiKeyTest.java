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

package com.bytechef.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.config.ApplicationProperties.Ai.Provider;
import org.junit.jupiter.api.Test;

class AiProviderApiKeyTest {

    @Test
    void testGetProviderApiKeyReturnsConfiguredKey() {
        Provider provider = new Provider();

        provider.getOpenAi()
            .setApiKey("sk-open-ai");

        assertEquals("sk-open-ai", provider.getProviderApiKey("ai.provider.openAi"));
    }

    @Test
    void testGetProviderApiKeyReturnsConfiguredKeyWhenKeyDiffersFromFieldName() {
        Provider provider = new Provider();

        provider.getDeepSeek()
            .setApiKey("sk-deepseek");

        assertEquals("sk-deepseek", provider.getProviderApiKey("ai.provider.deepseek"));
    }

    @Test
    void testGetProviderApiKeyReturnsTheKeyOfEveryProvider() {
        Provider provider = new Provider();

        provider.getAnthropic()
            .setApiKey("anthropic-key");
        provider.getAzureOpenAi()
            .setApiKey("azure-open-ai-key");
        provider.getDeepSeek()
            .setApiKey("deepseek-key");
        provider.getGroq()
            .setApiKey("groq-key");
        provider.getMistral()
            .setApiKey("mistral-key");
        provider.getNvidia()
            .setApiKey("nvidia-key");
        provider.getOllama()
            .setApiKey("ollama-key");
        provider.getOpenAi()
            .setApiKey("open-ai-key");
        provider.getPerplexity()
            .setApiKey("perplexity-key");
        provider.getStability()
            .setApiKey("stability-key");
        provider.getVertexGemini()
            .setApiKey("vertex-gemini-key");

        assertEquals("anthropic-key", provider.getProviderApiKey("ai.provider.anthropic"));
        assertEquals("azure-open-ai-key", provider.getProviderApiKey("ai.provider.azureOpenAi"));
        assertEquals("deepseek-key", provider.getProviderApiKey("ai.provider.deepseek"));
        assertEquals("groq-key", provider.getProviderApiKey("ai.provider.groq"));
        assertEquals("mistral-key", provider.getProviderApiKey("ai.provider.mistral"));
        assertEquals("nvidia-key", provider.getProviderApiKey("ai.provider.nvidia"));
        assertEquals("ollama-key", provider.getProviderApiKey("ai.provider.ollama"));
        assertEquals("open-ai-key", provider.getProviderApiKey("ai.provider.openAi"));
        assertEquals("perplexity-key", provider.getProviderApiKey("ai.provider.perplexity"));
        assertEquals("stability-key", provider.getProviderApiKey("ai.provider.stability"));
        assertEquals("vertex-gemini-key", provider.getProviderApiKey("ai.provider.vertexGemini"));
    }

    @Test
    void testGetProviderApiKeyReturnsNullForUnknownKey() {
        Provider provider = new Provider();

        assertNull(provider.getProviderApiKey("ai.provider.unknown"));
    }
}
