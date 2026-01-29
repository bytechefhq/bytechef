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

import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * Service for accessing tags associated with KnowledgeBases.
 *
 * @author Ivica Cardic
 */
public interface KnowledgeBaseTagService {

    /**
     * Retrieves a list of all tags.
     *
     * @return a list of Tag objects representing all available tags
     */
    List<Tag> getAllTags();

    /**
     * Retrieves a mapping from knowledgebase ID to list of tags assigned to that knowledgeBase.
     *
     * @return a map where keys are knowledgebase IDs and values are lists of Tag objects assigned to each knowledgebase
     */
    Map<Long, List<Tag>> getTagsByKnowledgeBaseId();

    /**
     * Retrieves a mapping from knowledgebase name to list of tags assigned to that knowledgeBase.
     *
     * @return a map where keys are knowledgebase names and values are lists of Tag objects assigned to each
     *         knowledgebase
     */
    Map<String, List<Tag>> getTagsByKnowledgeBaseName();

    /**
     * Updates the tags associated with a specific knowledgeBase.
     *
     * @param knowledgeBaseId the unique identifier of the knowledgebase whose tags are to be updated
     * @param tags            a list of Tag objects representing the new set of tags to associate with the knowledgebase
     */
    void updateTags(long knowledgeBaseId, List<Tag> tags);
}
