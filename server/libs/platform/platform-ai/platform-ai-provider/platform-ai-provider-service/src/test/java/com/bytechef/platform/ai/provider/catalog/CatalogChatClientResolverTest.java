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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CatalogChatClientResolverTest {

    private static final String OPEN_AI_KEY = Provider.OPEN_AI.getKey();

    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);
    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogChatClientResolver resolver =
        new CatalogChatClientResolverImpl(catalogChatModelFactory, aiProviderFacade);

    @Test
    void testResolveReturnsNullForUnknownProviderKey() {
        assertThat(resolver.resolve("notAProvider", "x", 1)).isNull();
    }

    @Test
    void testResolveReturnsNullForOutOfRangeEnvironment() {
        // The range check short-circuits before any API-key lookup, so no stubbing is needed (and a forged or
        // out-of-range ordinal can never drive platform-API-key selection — fail closed).
        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 99)).isNull();
        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", -1)).isNull();
    }

    @Test
    void testResolveReturnsNullWhenNoApiKey() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn(null);

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNull();
    }

    @Test
    void testResolveBuildsClientWhenApiKeyResolves() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn("sk-test");
        when(catalogChatModelFactory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-test", null))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNotNull();
    }

    @Test
    void testResolveBuildsClientWhenApiKeyIsConfigFallback() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn("sk-config");
        when(catalogChatModelFactory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-config", null))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNotNull();
    }
}
