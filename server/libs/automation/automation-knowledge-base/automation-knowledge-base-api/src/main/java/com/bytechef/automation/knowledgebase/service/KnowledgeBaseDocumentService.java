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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.dto.DocumentStatusUpdate;
import java.util.List;

public interface KnowledgeBaseDocumentService {

    /**
     * Deletes the knowledge base document identified by the specified ID.
     *
     * @param id the unique identifier of the knowledge base document to be deleted
     */
    void delete(long id);

    /**
     * Retrieves the knowledge base document for the specified unique identifier.
     *
     * @param id the unique identifier of the knowledge base document to retrieve
     * @return the {@code KnowledgeBaseDocument} associated with the given ID, or {@code null} if no such document
     *         exists
     */
    KnowledgeBaseDocument getKnowledgeBaseDocument(long id);

    /**
     * Retrieves a list of knowledge base documents associated with the specified knowledge base ID.
     *
     * @param knowledgeBaseId the unique identifier of the knowledge base
     * @return a list of {@code KnowledgeBaseDocument} objects associated with the provided knowledge base ID
     */
    List<KnowledgeBaseDocument> getKnowledgeBaseDocuments(long knowledgeBaseId);

    /**
     * Retrieves the status of the knowledge base document identified by the specified ID.
     *
     * @param id the unique identifier of the knowledge base document whose status is to be retrieved
     * @return a {@code DocumentStatusUpdate} object containing the status information of the specified knowledge base
     *         document
     */
    DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id);

    /**
     * Saves the provided {@code KnowledgeBaseDocument} to the underlying data store. If the document already exists
     * (based on its unique identifier), it will be updated; otherwise, a new document will be created.
     *
     * @param knowledgeBaseDocument the {@code KnowledgeBaseDocument} to be saved or updated
     * @return the saved or updated {@code KnowledgeBaseDocument} instance, including any modifications made during the
     *         save operation (e.g., generated IDs, timestamps)
     */
    KnowledgeBaseDocument saveKnowledgeBaseDocument(KnowledgeBaseDocument knowledgeBaseDocument);
}
