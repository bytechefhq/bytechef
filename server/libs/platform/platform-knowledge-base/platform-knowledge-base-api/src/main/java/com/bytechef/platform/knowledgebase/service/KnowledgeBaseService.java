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

package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.owner.Owner;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

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
     * @return the KnowledgeBase object associated with the given ID
     * @throws RuntimeException if no knowledge base exists with the given ID
     */
    KnowledgeBase getKnowledgeBase(Long id);

    /**
     * Owner-aware form. An unowned knowledge base is readable by everyone, an empty owner reads everything, and an
     * owned one is readable only by that owner. A refusal is reported exactly as a missing knowledge base, so ids
     * cannot be probed for existence.
     *
     * @param id    the knowledge base id
     * @param owner the caller's owner, or empty for an admin or automation caller
     * @return the knowledge base
     */
    KnowledgeBase getKnowledgeBase(Long id, Optional<Owner> owner);

    /**
     * Retrieves a list of all available knowledge bases.
     *
     * @return a list of {@code KnowledgeBase} objects representing the knowledge bases.
     */
    List<KnowledgeBase> getKnowledgeBases();

    /**
     * Owner-aware form of the unscoped listing. See {@link #getKnowledgeBase(Long, Optional)}.
     */
    List<KnowledgeBase> getKnowledgeBases(Optional<Owner> owner);

    /**
     * Retrieves a list of knowledge bases for the specified environment.
     *
     * @param environment the environment ordinal to filter by
     * @return a list of {@code KnowledgeBase} objects in the given environment
     */
    List<KnowledgeBase> getKnowledgeBases(int environment);

    /**
     * Owner-aware form. See {@link #getKnowledgeBase(Long, Optional)}.
     */
    List<KnowledgeBase> getKnowledgeBases(int environment, Optional<Owner> owner);

    /**
     * Assigns a knowledge base to an account, or returns it to the vendor when {@code owner} is null.
     *
     * <p>
     * Documents inherit through {@code knowledge_base_id} and carry no owner of their own, so this one write moves the
     * whole knowledge base.
     *
     * @param id    the knowledge base id
     * @param owner the owning account, or null to make the knowledge base shared again
     */
    void assignOwner(long id, @Nullable Owner owner);

    /**
     * Updates an existing KnowledgeBase identified by the given ID with the provided new values.
     *
     * @param id            the unique identifier of the KnowledgeBase to update
     * @param knowledgeBase the KnowledgeBase object containing the updated values
     * @return the updated KnowledgeBase object after persisting the changes
     */
    KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase);
}
