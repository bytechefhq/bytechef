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

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import java.util.List;
import java.util.Map;

/**
 * Workspace-checked access to the documents and chunks of a knowledge base. Every operation resolves the owning
 * workspace from the document, chunk or knowledge base id and checks the caller's permission on it.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseDocumentFacade {

    /**
     * Deletes a document and its chunks.
     *
     * @param knowledgeBaseDocumentId the id of the document
     */
    void deleteKnowledgeBaseDocument(long knowledgeBaseDocumentId);

    /**
     * Deletes a document chunk.
     *
     * @param knowledgeBaseDocumentChunkId the id of the chunk
     */
    void deleteKnowledgeBaseDocumentChunk(long knowledgeBaseDocumentChunkId);

    /**
     * Retrieves a document.
     *
     * @param knowledgeBaseDocumentId the id of the document
     * @return the document
     */
    KnowledgeBaseDocument getKnowledgeBaseDocument(long knowledgeBaseDocumentId);

    /**
     * Retrieves the chunks of a document, with their content.
     *
     * @param knowledgeBaseDocumentId the id of the document
     * @return the chunks of the document
     */
    List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunks(long knowledgeBaseDocumentId);

    /**
     * Retrieves the processing status of a document.
     *
     * @param knowledgeBaseDocumentId the id of the document
     * @return the processing status
     */
    DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long knowledgeBaseDocumentId);

    /**
     * Retrieves the distinct tag names used by the documents of a knowledge base.
     *
     * @param knowledgeBaseId the id of the knowledge base
     * @return the tag names
     */
    List<String> getKnowledgeBaseDocumentTags(long knowledgeBaseId);

    /**
     * Retrieves a mapping from document id to tag names, for the documents of a knowledge base.
     *
     * @param knowledgeBaseId the id of the knowledge base
     * @return the tag names of each document
     */
    Map<Long, List<String>> getKnowledgeBaseDocumentTagsByDocumentId(long knowledgeBaseId);

    /**
     * Replaces the content of a document chunk.
     *
     * @param knowledgeBaseDocumentChunkId the id of the chunk
     * @param content                      the new content
     * @return the updated chunk
     */
    KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long knowledgeBaseDocumentChunkId, String content);

    /**
     * Replaces the tag names of a document.
     *
     * @param knowledgeBaseDocumentId the id of the document
     * @param tagNames                the new tag names
     */
    void updateKnowledgeBaseDocumentTags(long knowledgeBaseDocumentId, List<String> tagNames);
}
