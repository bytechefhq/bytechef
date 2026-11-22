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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Ivica Cardic
 */
class CopilotQueryEmbeddingModelTest {

    private static final String OPEN_AI_KEY = Provider.OPEN_AI.getKey();

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final List<String> createdForApiKeys = new CopyOnWriteArrayList<>();

    @AfterEach
    void afterEach() {
        EnvironmentContext.clear();
    }

    @Test
    void testEmbedsWithTheOpenAiKeyOfTheCurrentEnvironment() {
        EnvironmentContext.set(Environment.DEVELOPMENT);

        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, Environment.DEVELOPMENT.ordinal())).thenReturn("user-key");

        float[] embedding = queryEmbeddingModel().embed(new Document("ByteChef"));

        assertThat(embedding).containsExactly(0.5f);
        assertThat(createdForApiKeys).containsExactly("user-key");
    }

    @Test
    void testReusesTheModelUntilTheKeyChanges() {
        EnvironmentContext.set(Environment.DEVELOPMENT);

        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, Environment.DEVELOPMENT.ordinal()))
            .thenReturn("first-key", "first-key", "second-key");

        CopilotQueryEmbeddingModel queryEmbeddingModel = queryEmbeddingModel();

        queryEmbeddingModel.embed(new Document("one"));
        queryEmbeddingModel.embed(new Document("two"));
        queryEmbeddingModel.embed(new Document("three"));

        assertThat(createdForApiKeys).containsExactly("first-key", "second-key");
    }

    @Test
    void testFailsWithAProviderHintWhenTheUserHasNoKey() {
        EnvironmentContext.set(Environment.STAGING);

        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, Environment.STAGING.ordinal())).thenReturn("");

        CopilotQueryEmbeddingModel queryEmbeddingModel = queryEmbeddingModel();

        assertThatThrownBy(() -> queryEmbeddingModel.embed(new Document("ByteChef")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AI Providers")
            .hasMessageContaining("STAGING");

        assertThat(createdForApiKeys).isEmpty();
    }

    @Test
    void testReportsTheConfiguredDimensionsWithoutCallingTheProvider() {
        assertThat(queryEmbeddingModel().dimensions()).isEqualTo(1536);
        assertThat(createdForApiKeys).isEmpty();
    }

    @Test
    void testHasApiKeyIgnoresBlankKeys() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 0)).thenReturn(" ");
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn("user-key");

        assertThat(CopilotQueryEmbeddingModel.hasApiKey(aiProviderFacade, 0)).isFalse();
        assertThat(CopilotQueryEmbeddingModel.hasApiKey(aiProviderFacade, 1)).isTrue();
    }

    private CopilotQueryEmbeddingModel queryEmbeddingModel() {
        return new CopilotQueryEmbeddingModel(
            aiProviderFacade,
            apiKey -> {
                createdForApiKeys.add(apiKey);

                EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

                when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] {
                    0.5f
                });

                return embeddingModel;
            },
            1536);
    }
}
