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

package com.bytechef.platform.knowledgebase.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentServiceTest {

    private final KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository =
        mock(KnowledgeBaseDocumentRepository.class);
    private final KnowledgeBaseStorageService knowledgeBaseStorageService = mock(KnowledgeBaseStorageService.class);
    private final KnowledgeBaseDocumentServiceImpl knowledgeBaseDocumentService =
        new KnowledgeBaseDocumentServiceImpl(knowledgeBaseDocumentRepository, knowledgeBaseStorageService);

    @Test
    void testSaveNewDocumentChecksItsFullSize() {
        knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document(null, 500L));

        verify(knowledgeBaseStorageService).checkWithinLimit(500L);
    }

    @Test
    void testSaveGrowingDocumentChecksOnlyTheGrowth() {
        when(knowledgeBaseDocumentRepository.findById(1L)).thenReturn(Optional.of(document(1L, 300L)));

        knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document(1L, 500L));

        verify(knowledgeBaseStorageService).checkWithinLimit(200L);
    }

    @Test
    void testSaveShrinkingDocumentSkipsTheCheck() {
        when(knowledgeBaseDocumentRepository.findById(1L)).thenReturn(Optional.of(document(1L, 800L)));

        knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document(1L, 500L));

        verify(knowledgeBaseStorageService, never()).checkWithinLimit(anyLong());
    }

    @Test
    void testSaveDocumentWithoutSizeSkipsTheCheck() {
        knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document(null, null));

        verify(knowledgeBaseStorageService, never()).checkWithinLimit(anyLong());
    }

    private static KnowledgeBaseDocument document(Long id, Long documentSize) {
        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setId(id);
        knowledgeBaseDocument.setDocumentSize(documentSize);

        return knowledgeBaseDocument;
    }
}
