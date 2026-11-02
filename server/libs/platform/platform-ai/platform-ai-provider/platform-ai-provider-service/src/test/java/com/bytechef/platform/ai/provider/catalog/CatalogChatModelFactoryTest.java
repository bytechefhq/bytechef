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

package com.bytechef.platform.ai.provider.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CatalogChatModelFactoryTest {

    private final CatalogChatModelFactory factory = new CatalogChatModelFactory();

    @Test
    void testCreateChatModelForOpenAiReturnsModel() {
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-test-key", null);

        assertThat(chatModel).isNotNull();
    }

    @Test
    void testCreateChatModelForAnthropicUsesComponentDefaultMaxTokens() {
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.ANTHROPIC, "claude-sonnet-5", "sk-test-key", null);

        assertThat(chatModel).isNotNull();
        assertThat(chatModel.getDefaultOptions()
            .getMaxTokens()).isEqualTo(AnthropicConstants.DEFAULT_MAX_TOKENS);
    }

    @Test
    void testCreateChatModelForAzureWithoutEndpointReturnsNull() {
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.AZURE_OPEN_AI, "gpt-4o", "sk-test-key", null);

        assertThat(chatModel).isNull();
    }

    @Test
    void testCreateChatModelForAzureWithEndpointReturnsModel() {
        org.springframework.ai.chat.model.ChatModel chatModel = factory.createChatModel(
            Provider.AZURE_OPEN_AI, "gpt-4o", "sk-test-key", "https://test-resource.openai.azure.com");

        assertThat(chatModel).isNotNull();
    }

    @Test
    void testCreateChatModelForOllamaWithCustomUrlReturnsModel() {
        // Ollama needs no API key and accepts a custom base URL threaded through the connection parameters.
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.OLLAMA, "llama3", null, "http://remote-host:11434");

        assertThat(chatModel).isNotNull();
    }
}
