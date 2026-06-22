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
import static com.bytechef.platform.ai.llm.Provider.OPEN_AI;
import static com.bytechef.platform.ai.llm.Provider.PERPLEXITY;
import static com.bytechef.platform.ai.llm.Provider.STABILITY;
import static com.bytechef.platform.ai.llm.Provider.VERTEX_GEMINI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.platform.ai.llm.Provider;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LLMTextProviderRegistryTest {

    @Test
    void testAllTextProvidersResolveToAChatModel() {
        List<Provider> textProviders = List.of(
            ANTHROPIC, AZURE_OPEN_AI, DEEPSEEK, GROQ, MISTRAL, NVIDIA, OPEN_AI, PERPLEXITY, VERTEX_GEMINI);

        for (Provider provider : textProviders) {
            assertNotNull(LLMModelRegistry.getChatModel(provider), "No chat model for " + provider);
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
