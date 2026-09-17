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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseStorageService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseVectorStoreMetadataService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentFacadeTest {

    private final KnowledgeBaseFileStorage fileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseDocumentService documentService = mock(KnowledgeBaseDocumentService.class);
    private final KnowledgeBaseStorageService storageService = mock(KnowledgeBaseStorageService.class);

    private KnowledgeBaseDocumentFacadeImpl createFacade() {
        return new KnowledgeBaseDocumentFacadeImpl(
            mock(ApplicationEventPublisher.class), mock(KnowledgeBaseDocumentChunkService.class), documentService,
            mock(KnowledgeBaseDocumentTagService.class), fileStorage, mock(KnowledgeBaseService.class),
            mock(KnowledgeBaseVectorStoreMetadataService.class), mock(VectorStore.class), storageService);
    }

    @Test
    void testCreateBlockedWhenOverLimit() {
        doThrow(new KnowledgeBaseStorageLimitExceededException(2_000L, 1_000L))
            .when(storageService)
            .checkWithinLimit(500);

        assertThatThrownBy(() -> createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);

        verifyNoInteractions(fileStorage);
    }

    @Test
    void testCreatePersistsDocumentSize() {
        when(fileStorage.storeDocument(eq("a.txt"), any())).thenReturn(mock(FileEntry.class));
        when(documentService.saveKnowledgeBaseDocument(any())).thenAnswer(invocation -> invocation.getArgument(0));

        createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0]));

        ArgumentCaptor<KnowledgeBaseDocument> captor = ArgumentCaptor.forClass(KnowledgeBaseDocument.class);

        verify(documentService).saveKnowledgeBaseDocument(captor.capture());

        assertThat(captor.getValue()
            .getDocumentSize())
                .isEqualTo(500L);
    }
}
