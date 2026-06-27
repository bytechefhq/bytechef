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

package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import tools.jackson.databind.ObjectMapper;

class KnowledgeBaseFacadeImplTest {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final VectorStore vectorStore = mock(VectorStore.class, RETURNS_DEEP_STUBS);

    private final KnowledgeBaseFacadeImpl facade = new KnowledgeBaseFacadeImpl(
        knowledgeBaseDocumentChunkService, knowledgeBaseFileStorage, knowledgeBaseService, objectMapper, vectorStore);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testSetsEnvironmentDuringSearchAndClearsAfter() {
        AtomicReference<Environment> observed = new AtomicReference<>();
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setEnvironment(Environment.STAGING);

        when(knowledgeBaseService.getKnowledgeBase(1L)).thenReturn(knowledgeBase);
        doAnswer(invocation -> {
            observed.set(EnvironmentContext.getCurrentEnvironment());

            return List.of();
        }).when(vectorStore)
            .similaritySearch(org.mockito.ArgumentMatchers.any(org.springframework.ai.vectorstore.SearchRequest.class));

        facade.searchKnowledgeBase(1L, "query", null);

        assertThat(observed.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
