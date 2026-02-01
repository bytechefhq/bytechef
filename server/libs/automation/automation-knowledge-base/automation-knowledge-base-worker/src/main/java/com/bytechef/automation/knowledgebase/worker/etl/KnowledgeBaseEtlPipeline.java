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

package com.bytechef.automation.knowledgebase.worker.etl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * ETL Pipeline for processing knowledge base documents. Orchestrates the Extract-Transform-Load process using Spring AI
 * components.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseEtlPipeline {

    private final KnowledgeBaseDocumentReaderFactory documentReaderFactory;
    private final KnowledgeBaseDocumentTransformerChain transformerChain;
    private final KnowledgeBaseVectorStoreWriter vectorStoreWriter;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseEtlPipeline(
        KnowledgeBaseDocumentReaderFactory documentReaderFactory,
        KnowledgeBaseDocumentTransformerChain transformerChain, KnowledgeBaseVectorStoreWriter vectorStoreWriter) {

        this.documentReaderFactory = documentReaderFactory;
        this.transformerChain = transformerChain;
        this.vectorStoreWriter = vectorStoreWriter;
    }

    /**
     * Processes a document through the ETL pipeline (Extract and Transform only, no Load).
     *
     * @param resource          the document resource
     * @param mimeType          the MIME type of the document
     * @param minChunkSizeChars the minimum chunk size in characters for splitting
     * @param maxChunkSize      the maximum chunk size for splitting
     * @param overlap           the number of characters to overlap between chunks (0 for no overlap)
     * @return the list of processed documents (not yet stored in vector store)
     */
    public List<Document> process(
        Resource resource, String mimeType, int minChunkSizeChars, int maxChunkSize, int overlap) {

        // Extract: Read documents from the source
        DocumentReader documentReader = documentReaderFactory.createDocumentReader(resource, mimeType);

        List<Document> documents = documentReader.get();

        // Transform: Split documents into chunks
        return transformerChain.transform(documents, minChunkSizeChars, maxChunkSize, overlap);
    }

    /**
     * Writes a single chunk to the vector store with all metadata including chunk ID.
     *
     * @param document        the document to write
     * @param knowledgeBaseId the knowledge base ID
     * @param documentId      the knowledge base document ID
     * @param chunkId         the knowledge base document chunk ID
     * @param tagIds          the tag IDs associated with the document
     * @return the vector store ID assigned to the document
     */
    public String writeChunkToVectorStore(
        Document document, Long knowledgeBaseId, Long documentId, Long chunkId, List<Long> tagIds) {

        vectorStoreWriter.writeChunk(document, knowledgeBaseId, documentId, chunkId, tagIds);

        return document.getId();
    }

    /**
     * Processes a single chunk update through the pipeline.
     *
     * @param content         the chunk content
     * @param knowledgeBaseId the knowledge base ID
     * @param documentId      the knowledge base document ID
     * @param chunkId         the knowledge base document chunk ID
     * @param tagIds          the tag IDs associated with the document
     */
    public void processChunkUpdate(
        String content, Long knowledgeBaseId, Long documentId, Long chunkId, List<Long> tagIds) {

        Document document = new Document(content);

        vectorStoreWriter.writeChunk(document, knowledgeBaseId, documentId, chunkId, tagIds);
    }
}
