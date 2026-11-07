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

package com.bytechef.component.ai.universal.text;

import static com.bytechef.platform.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.platform.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.platform.ai.llm.Provider.DEEPSEEK;
import static com.bytechef.platform.ai.llm.Provider.GROQ;
import static com.bytechef.platform.ai.llm.Provider.HUGGING_FACE;
import static com.bytechef.platform.ai.llm.Provider.MISTRAL;
import static com.bytechef.platform.ai.llm.Provider.NVIDIA;
import static com.bytechef.platform.ai.llm.Provider.OLLAMA;
import static com.bytechef.platform.ai.llm.Provider.OPEN_AI;
import static com.bytechef.platform.ai.llm.Provider.PERPLEXITY;
import static com.bytechef.platform.ai.llm.Provider.STABILITY;
import static com.bytechef.platform.ai.llm.Provider.VERTEX_GEMINI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiChatAction;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.gemini.action.GeminiChatAction;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.ollama.action.OllamaChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.platform.ai.llm.Provider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LLMTextProviderRegistryTest {

    private static final Map<Provider, ChatModel> SWITCH_CHAT_MODELS = new EnumMap<>(Map.of(
        ANTHROPIC, AnthropicChatAction.CHAT_MODEL,
        AZURE_OPEN_AI, AzureOpenAiChatAction.CHAT_MODEL,
        DEEPSEEK, DeepSeekChatAction.CHAT_MODEL,
        GROQ, GroqChatAction.CHAT_MODEL,
        MISTRAL, MistralChatAction.CHAT_MODEL,
        NVIDIA, NvidiaChatAction.CHAT_MODEL,
        OLLAMA, OllamaChatAction.CHAT_MODEL,
        OPEN_AI, OpenAiChatAction.CHAT_MODEL,
        PERPLEXITY, PerplexityChatAction.CHAT_MODEL,
        VERTEX_GEMINI, GeminiChatAction.CHAT_MODEL));

    @Test
    void testAllTextProvidersResolveToAChatModel() {
        List<Provider> textProviders = List.of(
            ANTHROPIC, AZURE_OPEN_AI, DEEPSEEK, GROQ, MISTRAL, NVIDIA, OLLAMA, OPEN_AI, PERPLEXITY, VERTEX_GEMINI);

        for (Provider provider : textProviders) {
            assertNotNull(LLMModelRegistry.getChatModel(provider), "No chat model for " + provider);
        }
    }

    @Test
    void testEveryProviderResolvesToTheChatModelOfTheReplacedSwitch() {
        for (Provider provider : Provider.values()) {
            ChatModel expectedChatModel = SWITCH_CHAT_MODELS.get(provider);

            assertEquals(expectedChatModel != null, LLMModelRegistry.hasChatModel(provider), provider.name());

            if (expectedChatModel == null) {
                assertThrows(IllegalArgumentException.class, () -> LLMModelRegistry.getChatModel(provider));
            } else {
                assertSame(expectedChatModel, LLMModelRegistry.getChatModel(provider), provider.name());
            }
        }
    }

    @Test
    void testGroqResolvesToItsOwnChatModel() {
        assertSame(GroqChatAction.CHAT_MODEL, LLMModelRegistry.getChatModel(GROQ));
    }

    @Test
    void testHasChatModelReflectsChatCapability() {
        assertTrue(LLMModelRegistry.hasChatModel(OPEN_AI));
        assertFalse(LLMModelRegistry.hasChatModel(STABILITY));
        assertFalse(LLMModelRegistry.hasChatModel(HUGGING_FACE));
    }
}
