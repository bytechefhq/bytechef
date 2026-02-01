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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentChunkEvent;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseDocumentChunkFacadeImpl implements KnowledgeBaseDocumentChunkFacade {

    private final ApplicationEventPublisher eventPublisher;
    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentChunkFacadeImpl(
        ApplicationEventPublisher eventPublisher, KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore) {

        this.eventPublisher = eventPublisher;
        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(long knowledgeBaseDocumentId) {
        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId);

        for (KnowledgeBaseDocumentChunk chunk : chunks) {
            FileEntry contentFileEntry = chunk.getContent();

            if (contentFileEntry != null) {
                String textContent = knowledgeBaseFileStorage.readChunkContent(contentFileEntry);

                chunk.setTextContent(textContent);
            }
        }

        return chunks;
    }

    @Override
    public KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long id, String content) {
        KnowledgeBaseDocumentChunk existingChunk = knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(id);

        FileEntry oldContentFileEntry = existingChunk.getContent();

        if (oldContentFileEntry != null) {
            knowledgeBaseFileStorage.deleteChunkContent(oldContentFileEntry);
        }

        FileEntry newContentFileEntry = knowledgeBaseFileStorage.storeChunkContent(id, content);

        existingChunk.setContent(newContentFileEntry);
        existingChunk.setTextContent(content);

        knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(existingChunk);

        eventPublisher.publishEvent(new KnowledgeBaseDocumentChunkEvent(existingChunk.getId(), content));

        return existingChunk;
    }

    @Override
    public void deleteKnowledgeBaseDocumentChunk(long id) {
        KnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(id);

        deleteChunkResources(chunk);

        knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunk(id);
    }

    private void deleteChunkResources(KnowledgeBaseDocumentChunk chunk) {
        String vectorStoreId = chunk.getVectorStoreId();

        if (vectorStoreId != null) {
            vectorStore.delete(List.of(vectorStoreId));
        }

        FileEntry contentFileEntry = chunk.getContent();

        if (contentFileEntry != null) {
            knowledgeBaseFileStorage.deleteChunkContent(contentFileEntry);
        }
    }
}
