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
import java.io.InputStream;

/**
 * Facade for managing knowledge base documents including file storage operations.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseDocumentFacade {

    /**
     * Creates a new knowledge base document by storing the file and creating the document record.
     *
     * @param knowledgeBaseId the ID of the knowledge base to add the document to
     * @param filename        the name of the file
     * @param contentType     the content type of the file
     * @param inputStream     the input stream of the file content
     * @return the created knowledge base document
     */
    KnowledgeBaseDocument createKnowledgeBaseDocument(
        Long knowledgeBaseId, String filename, String contentType, InputStream inputStream);

    /**
     * Deletes a knowledge base document including its stored file.
     *
     * @param id the ID of the document to delete
     */
    void deleteKnowledgeBaseDocument(Long id);
}
