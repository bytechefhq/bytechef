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
 * Facade for knowledge base search operations.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseFacade {

    /**
     * Searches the knowledge base using a query string.
     *
     * @param knowledgeBaseId the ID of the knowledge base to search
     * @param query           the search query
     * @param metadataFilters optional metadata filters
     * @return list of matching chunks
     */
    List<KnowledgeBaseDocumentChunk> searchKnowledgeBase(Long knowledgeBaseId, String query, String metadataFilters);
}
