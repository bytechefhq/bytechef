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

package com.bytechef.platform.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiProviderEmbeddingProviderStatusProviderTest {

    private static final AiDefaultModelWithApiKeyDTO OPEN_AI_DEFAULT_MODEL =
        new AiDefaultModelWithApiKeyDTO(Provider.OPEN_AI, "text-embedding-3-small", "sk-test", null);

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final AiProviderEmbeddingProviderStatusProvider statusProvider =
        new AiProviderEmbeddingProviderStatusProvider(aiProviderFacade);

    @Test
    void testActiveWhenDefaultModelResolves() {
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(2)).thenReturn(OPEN_AI_DEFAULT_MODEL);

        assertThat(statusProvider.isEmbeddingActive(2)).isTrue();
    }

    @Test
    void testInactiveWhenNoDefaultModel() {
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(2)).thenReturn(null);

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }
}
