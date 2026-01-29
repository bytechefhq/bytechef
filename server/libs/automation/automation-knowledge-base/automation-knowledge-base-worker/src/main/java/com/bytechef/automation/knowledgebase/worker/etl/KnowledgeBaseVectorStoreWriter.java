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
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writer for storing knowledge base documents in PgVector store.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
public class KnowledgeBaseVectorStoreWriter {

    public static final String METADATA_KNOWLEDGE_BASE_ID = "knowledge_base_id";
    public static final String METADATA_KNOWLEDGE_BASE_DOCUMENT_ID = "knowledge_base_document_id";
    public static final String METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID = "knowledge_base_document_chunk_id";
    public static final String METADATA_TAG_IDS = "tag_ids";

    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseVectorStoreWriter(@Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Writes documents to the vector store with knowledge base metadata.
     *
     * @param documents               the documents to write
     * @param knowledgeBaseId         the knowledge base ID
     * @param knowledgeBaseDocumentId the knowledge base document ID
     * @param tagIds                  the tag IDs associated with the document
     */
    public void write(
        List<Document> documents, long knowledgeBaseId, long knowledgeBaseDocumentId, List<Long> tagIds) {

        List<Document> sanitizedDocuments = documents.stream()
            .map(document -> sanitizeDocument(document, knowledgeBaseId, knowledgeBaseDocumentId, -1, tagIds))
            .toList();

        vectorStore.add(sanitizedDocuments);
    }

    /**
     * Writes a single document (chunk) to the vector store with knowledge base metadata.
     *
     * @param document        the document to write
     * @param knowledgeBaseId the knowledge base ID
     * @param documentId      the knowledge base document ID
     * @param chunkId         the knowledge base document chunk ID
     * @param tagIds          the tag IDs associated with the document
     */
    public void writeChunk(
        Document document, Long knowledgeBaseId, Long documentId, Long chunkId, List<Long> tagIds) {

        Document sanitizedDocument = sanitizeDocument(document, knowledgeBaseId, documentId, chunkId, tagIds);

        vectorStore.add(List.of(sanitizedDocument));
    }

    /**
     * Deletes documents by their vector store IDs.
     *
     * @param vectorStoreIds the vector store document IDs to delete
     */
    public void delete(List<String> vectorStoreIds) {
        vectorStore.delete(vectorStoreIds);
    }

    /**
     * Returns the underlying vector store for advanced operations.
     *
     * @return the vector store
     */
    @SuppressFBWarnings("EI")
    public VectorStore getVectorStore() {
        return vectorStore;
    }

    /**
     * Sanitizes a document by removing null bytes from content and adding knowledge base metadata. PostgreSQL text
     * columns don't support null bytes (0x00).
     */
    private Document sanitizeDocument(
        Document document, long knowledgeBaseId, long knowledgeBaseDocumentId, long knowledgeBaseDocumentChunkId,
        List<Long> tagIds) {

        String content = document.getText();

        if (content != null) {
            content = content.replace("\0", "");
        }

        Map<String, Object> metadata = new java.util.HashMap<>(document.getMetadata());

        metadata.put(METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);
        metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId);

        if (knowledgeBaseDocumentChunkId != -1) {
            metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId);
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            metadata.put(METADATA_TAG_IDS, tagIds);
        }

        return new Document(document.getId(), content, metadata);
    }
}
