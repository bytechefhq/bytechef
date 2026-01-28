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

package com.bytechef.automation.knowledgebase.worker;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentChunkEvent;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.worker.etl.KnowledgeBaseEtlPipeline;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Worker for processing knowledge base documents using ETL Pipeline.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentProcessWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeBaseDocumentProcessWorker.class);

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseEtlPipeline knowledgeBaseEtlPipeline;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;
    private final KnowledgeBaseService knowledgeBaseService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentProcessWorker(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseEtlPipeline knowledgeBaseEtlPipeline, KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService) {

        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseEtlPipeline = knowledgeBaseEtlPipeline;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public void onKnowledgeBaseDocumentEvent(KnowledgeBaseDocumentEvent event) {
        long knowledgeBaseDocumentId = event.getDocumentId();

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            knowledgeBaseDocumentId);

        try {
            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

            knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

            KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(
                knowledgeBaseDocument.getKnowledgeBaseId());
            FileEntry fileEntry = knowledgeBaseDocument.getDocument();

            byte[] documentBytes = knowledgeBaseFileStorage.readDocumentToBytes(fileEntry);

            Resource resource = new KnowledgeBaseDocumentByteArrayResource(documentBytes, fileEntry);

            List<Long> tagIds = knowledgeBaseDocument.getTagIds();

            List<Document> documents = knowledgeBaseEtlPipeline.process(
                resource, fileEntry.getMimeType(),
                knowledgeBase.getMinChunkSizeChars(), knowledgeBase.getMaxChunkSize(), knowledgeBase.getOverlap());

            for (Document document : documents) {
                KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

                knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);

                knowledgeBaseDocumentChunk =
                    knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunk);

                String vectorStoreId = knowledgeBaseEtlPipeline.writeChunkToVectorStore(
                    document, knowledgeBase.getId(), knowledgeBaseDocumentId, knowledgeBaseDocumentChunk.getId(),
                    tagIds);

                knowledgeBaseDocumentChunk.setVectorStoreId(vectorStoreId);

                FileEntry chunkContentFileEntry = knowledgeBaseFileStorage.storeChunkContent(
                    knowledgeBaseDocumentChunk.getId(), document.getText());

                knowledgeBaseDocumentChunk.setContent(chunkContentFileEntry);

                knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunk);
            }

            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_READY);

            knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Error processing document {}: {}", knowledgeBaseDocumentId, exception.getMessage(), exception);

            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_ERROR);

            knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);
        }
    }

    public void onKnowledgeBaseDocumentChunkEvent(KnowledgeBaseDocumentChunkEvent event) {
        long knowledgeBaseDocumentChunkId = event.getChunkId();

        try {
            KnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(
                knowledgeBaseDocumentChunkId);

            KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
                chunk.getKnowledgeBaseDocumentId());

            List<Long> tagIds = knowledgeBaseDocument.getTagIds();

            knowledgeBaseEtlPipeline.processChunkUpdate(
                event.getContent(), knowledgeBaseDocument.getKnowledgeBaseId(), knowledgeBaseDocument.getId(),
                knowledgeBaseDocumentChunkId, tagIds);
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Error processing chunk update {}: {}", knowledgeBaseDocumentChunkId, exception.getMessage(),
                exception);
        }
    }

    private static class KnowledgeBaseDocumentByteArrayResource extends ByteArrayResource {

        private final FileEntry fileEntry;

        public KnowledgeBaseDocumentByteArrayResource(byte[] documentBytes, FileEntry fileEntry) {
            super(documentBytes);

            this.fileEntry = fileEntry;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof KnowledgeBaseDocumentByteArrayResource that)) {
                return false;
            }

            if (!super.equals(object)) {
                return false;
            }

            return Objects.equals(fileEntry, that.fileEntry);
        }

        @Override
        public String getFilename() {
            return fileEntry.getName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fileEntry);
        }
    }
}
