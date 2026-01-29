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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.List;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
class KnowledgeBaseDocumentFacadeImpl implements KnowledgeBaseDocumentFacade {

    private final ApplicationEventPublisher eventPublisher;
    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentFacadeImpl(
        ApplicationEventPublisher eventPublisher, KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore) {

        this.eventPublisher = eventPublisher;
        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
        this.vectorStore = vectorStore;
    }

    @Override
    public KnowledgeBaseDocument createKnowledgeBaseDocument(
        Long knowledgeBaseId, String filename, String contentType, InputStream inputStream) {

        FileEntry fileEntry = knowledgeBaseFileStorage.storeDocument(filename, inputStream);

        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBaseId);
        knowledgeBaseDocument.setName(filename);
        knowledgeBaseDocument.setDocument(fileEntry);
        knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        knowledgeBaseDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

        eventPublisher.publishEvent(new KnowledgeBaseDocumentEvent(knowledgeBaseDocument.getId()));

        return knowledgeBaseDocument;
    }

    @Override
    public void deleteKnowledgeBaseDocument(Long id) {
        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(id);

        List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(id);

        List<String> vectorStoreIds = knowledgeBaseDocumentChunks.stream()
            .map(KnowledgeBaseDocumentChunk::getVectorStoreId)
            .filter(vectorStoreId -> vectorStoreId != null)
            .toList();

        if (!vectorStoreIds.isEmpty()) {
            vectorStore.delete(vectorStoreIds);
        }

        for (KnowledgeBaseDocumentChunk chunk : knowledgeBaseDocumentChunks) {
            FileEntry contentFileEntry = chunk.getContent();

            if (contentFileEntry != null) {
                knowledgeBaseFileStorage.deleteChunkContent(contentFileEntry);
            }
        }

        knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunks(knowledgeBaseDocumentChunks);

        knowledgeBaseFileStorage.deleteDocument(knowledgeBaseDocument.getDocument());

        knowledgeBaseDocumentService.delete(id);
    }
}
