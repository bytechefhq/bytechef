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

package com.bytechef.automation.data.table.configuration.service;

import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * Service for accessing tags associated with Data Tables.
 *
 * @author Ivica Cardic
 */
public interface DataTableTagService {

    /**
     * Retrieves a list of all tags.
     *
     * @return a list of Tag objects representing all available tags
     */
    List<Tag> getAllTags();

    /**
     * Retrieves a mapping from data table base name to list of tags assigned to that table.
     *
     * @return a map where keys are data table base names and values are lists of Tag objects assigned to each table
     */
    Map<String, List<Tag>> getTagsByTableName();

    /**
     * Updates the tags associated with a specific data table.
     *
     * @param tableId the unique identifier of the data table whose tags are to be updated
     * @param tags    a list of Tag objects representing the new set of tags to associate with the data table
     */
    void updateTags(long tableId, List<Tag> tags);
}
