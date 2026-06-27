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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class KnowledgeBaseGraphQlControllerEmbeddingActiveTest {

    @Test
    @SuppressWarnings("unchecked")
    void testReturnsStatusFromProvider() {
        EmbeddingProviderStatusProvider statusProvider = mock(EmbeddingProviderStatusProvider.class);
        ObjectProvider<EmbeddingProviderStatusProvider> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(statusProvider);
        when(statusProvider.isEmbeddingActive(2)).thenReturn(false);

        assertThat(KnowledgeBaseGraphQlController.resolveEmbeddingActive(objectProvider, 2)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDefaultsToActiveWhenNoProvider() {
        ObjectProvider<EmbeddingProviderStatusProvider> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        assertThat(KnowledgeBaseGraphQlController.resolveEmbeddingActive(objectProvider, 2)).isTrue();
    }
}
