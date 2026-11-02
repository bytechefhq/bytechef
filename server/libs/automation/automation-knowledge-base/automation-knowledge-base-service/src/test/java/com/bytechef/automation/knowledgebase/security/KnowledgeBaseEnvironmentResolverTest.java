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

package com.bytechef.automation.knowledgebase.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseEnvironmentResolverTest {

    private static final long CHUNK_ID = 3L;
    private static final long DOCUMENT_ID = 2L;
    private static final long KNOWLEDGE_BASE_ID = 1L;

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);

    private final KnowledgeBaseEnvironmentResolver knowledgeBaseEnvironmentResolver =
        new KnowledgeBaseEnvironmentResolver(knowledgeBaseService);
    private final KnowledgeBaseDocumentEnvironmentResolver knowledgeBaseDocumentEnvironmentResolver =
        new KnowledgeBaseDocumentEnvironmentResolver(knowledgeBaseDocumentService, knowledgeBaseEnvironmentResolver);
    private final KnowledgeBaseDocumentChunkEnvironmentResolver knowledgeBaseDocumentChunkEnvironmentResolver =
        new KnowledgeBaseDocumentChunkEnvironmentResolver(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentEnvironmentResolver);

    @BeforeEach
    void setUp() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setEnvironment(Environment.PRODUCTION);

        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setKnowledgeBaseId(KNOWLEDGE_BASE_ID);

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

        knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(DOCUMENT_ID);

        when(knowledgeBaseService.getKnowledgeBase(KNOWLEDGE_BASE_ID)).thenReturn(knowledgeBase);
        when(knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(DOCUMENT_ID))
            .thenReturn(Optional.of(knowledgeBaseDocument));
        when(knowledgeBaseDocumentChunkService.fetchKnowledgeBaseDocumentChunk(CHUNK_ID))
            .thenReturn(Optional.of(knowledgeBaseDocumentChunk));
    }

    @Test
    void testKnowledgeBaseReportsItsEnvironment() {
        assertThat(knowledgeBaseEnvironmentResolver.resourceType()).isEqualTo("KnowledgeBase");
        assertThat(knowledgeBaseEnvironmentResolver.fetchEnvironment(KNOWLEDGE_BASE_ID))
            .contains(Environment.PRODUCTION);
    }

    @Test
    void testDocumentReportsTheEnvironmentOfItsKnowledgeBase() {
        assertThat(knowledgeBaseDocumentEnvironmentResolver.resourceType()).isEqualTo("KnowledgeBaseDocument");
        assertThat(knowledgeBaseDocumentEnvironmentResolver.fetchEnvironment(DOCUMENT_ID))
            .contains(Environment.PRODUCTION);
        assertThat(knowledgeBaseDocumentEnvironmentResolver.fetchEnvironment(DOCUMENT_ID + 100)).isEmpty();
    }

    @Test
    void testChunkReportsTheEnvironmentOfItsKnowledgeBase() {
        assertThat(knowledgeBaseDocumentChunkEnvironmentResolver.resourceType())
            .isEqualTo("KnowledgeBaseDocumentChunk");
        assertThat(knowledgeBaseDocumentChunkEnvironmentResolver.fetchEnvironment(CHUNK_ID))
            .contains(Environment.PRODUCTION);
        assertThat(knowledgeBaseDocumentChunkEnvironmentResolver.fetchEnvironment(CHUNK_ID + 100)).isEmpty();
    }
}
