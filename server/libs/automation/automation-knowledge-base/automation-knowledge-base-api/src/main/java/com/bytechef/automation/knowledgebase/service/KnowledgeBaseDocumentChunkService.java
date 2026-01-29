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

package com.bytechef.automation.knowledgebase.service;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing knowledge base document chunks.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseDocumentChunkService {

    /**
     * Retrieves a specific KnowledgeBaseDocumentChunk by its unique identifier.
     *
     * @param id the unique identifier of the KnowledgeBaseDocumentChunk to retrieve
     * @return the KnowledgeBaseDocumentChunk associated with the given identifier, or null if no matching chunk is
     *         found
     */
    KnowledgeBaseDocumentChunk getKnowledgeBaseDocumentChunk(Long id);

    /**
     * Retrieves a specific {@code KnowledgeBaseDocumentChunk} based on its associated vector store identifier.
     *
     * @param vectorStoreId the unique identifier associated with the vector store entry of the
     *                      {@code KnowledgeBaseDocumentChunk}
     * @return an {@code Optional} containing the {@code KnowledgeBaseDocumentChunk} if found; otherwise, an empty
     *         {@code Optional}
     */
    Optional<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunkByVectorStoreId(String vectorStoreId);

    /**
     * Retrieves a list of {@code KnowledgeBaseDocumentChunk} objects based on their unique identifiers.
     *
     * @param ids a list of unique identifiers corresponding to the {@code KnowledgeBaseDocumentChunk} objects to
     *            retrieve
     * @return a list of {@code KnowledgeBaseDocumentChunk} objects associated with the specified identifiers
     */
    List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunks(List<Long> ids);

    /**
     * Retrieves a list of {@code KnowledgeBaseDocumentChunk} objects associated with the specified document ID.
     *
     * @param documentId the unique identifier of the knowledge base document whose chunks are to be retrieved
     * @return a list of {@code KnowledgeBaseDocumentChunk} objects corresponding to the given document ID
     */
    List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(Long documentId);

    /**
     * Persists the given {@code KnowledgeBaseDocumentChunk} into the storage system.
     *
     * @param knowledgeBaseDocumentChunk the {@code KnowledgeBaseDocumentChunk} instance to be saved
     * @return the saved {@code KnowledgeBaseDocumentChunk} with updated state and identifiers, if applicable
     */
    KnowledgeBaseDocumentChunk saveKnowledgeBaseDocumentChunk(KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk);

    /**
     * Persists a list of {@code KnowledgeBaseDocumentChunk} objects into the storage system.
     *
     * @param knowledgeBaseDocumentChunks the list of {@code KnowledgeBaseDocumentChunk} instances to be saved
     * @return the saved list of {@code KnowledgeBaseDocumentChunk} objects with updated state and identifiers, if
     *         applicable
     */
    List<KnowledgeBaseDocumentChunk> saveKnowledgeBaseDocumentChunks(
        List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks);

    /**
     * Deletes a specific {@code KnowledgeBaseDocumentChunk} identified by its unique identifier.
     *
     * @param id the unique identifier of the {@code KnowledgeBaseDocumentChunk} to be deleted
     */
    void deleteKnowledgeBaseDocumentChunk(Long id);

    /**
     * Deletes a list of {@code KnowledgeBaseDocumentChunk} objects.
     *
     * @param knowledgeBaseDocumentChunks the list of {@code KnowledgeBaseDocumentChunk} instances to be deleted
     */
    void deleteKnowledgeBaseDocumentChunks(List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks);
}
