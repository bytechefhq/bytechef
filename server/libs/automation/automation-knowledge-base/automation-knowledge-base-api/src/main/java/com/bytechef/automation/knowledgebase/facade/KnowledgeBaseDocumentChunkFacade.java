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
import java.util.List;

/**
 * Facade for managing knowledge base document chunks.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseDocumentChunkFacade {

    /**
     * Gets all chunks for a document with content populated from the vector store.
     *
     * @param knowledgeBaseDocumentId the knowledge base document ID
     * @return list of chunks with content
     */
    List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(long knowledgeBaseDocumentId);

    /**
     * Updates a knowledge base document chunk, recomputing embeddings if content changed.
     *
     * @param id      the ID of the chunk to update
     * @param content the new content
     * @return the updated chunk
     */
    KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long id, String content);

    /**
     * Deletes a knowledge base document chunk.
     *
     * @param id the ID of the chunk to delete
     */
    void deleteKnowledgeBaseDocumentChunk(long id);
}
