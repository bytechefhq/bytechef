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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import java.util.List;

public interface KnowledgeBaseService {

    /**
     * Creates a new KnowledgeBase instance and persists it.
     *
     * @param knowledgeBase the KnowledgeBase object to be created
     * @return the newly created KnowledgeBase object with its generated attributes populated
     */
    KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * Deletes the knowledge base identified by the specified ID.
     *
     * @param id the unique identifier of the knowledge base to be deleted
     */
    void deleteKnowledgeBase(Long id);

    /**
     * Retrieves the knowledge base associated with the specified unique identifier.
     *
     * @param id the unique identifier of the knowledge base to retrieve
     * @return the KnowledgeBase object associated with the given ID, or null if no such knowledge base exists
     */
    KnowledgeBase getKnowledgeBase(Long id);

    /**
     * Retrieves a list of all available knowledge bases.
     *
     * @return a list of {@code KnowledgeBase} objects representing the knowledge bases.
     */
    List<KnowledgeBase> getKnowledgeBases();

    /**
     * Updates an existing KnowledgeBase identified by the given ID with the provided new values.
     *
     * @param id            the unique identifier of the KnowledgeBase to update
     * @param knowledgeBase the KnowledgeBase object containing the updated values
     * @return the updated KnowledgeBase object after persisting the changes
     */
    KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase);
}
