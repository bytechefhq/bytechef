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

package com.bytechef.platform.data.table.configuration.service;

import java.util.List;
import java.util.Map;

/**
 * Service for accessing the tag ids associated with Data Tables.
 *
 * @author Ivica Cardic
 */
public interface DataTableTagService {

    /**
     * Retrieves the distinct tag ids assigned to the given data tables.
     *
     * @param dataTableIds the ids of the data tables whose tag ids are to be retrieved
     * @return a list of the tag ids assigned to the given data tables
     */
    List<Long> getTagIds(List<Long> dataTableIds);

    Map<Long, List<Long>> getTagIdsByTableId();

    /**
     * Updates the tag ids associated with a specific data table.
     *
     * @param tableId the unique identifier of the data table whose tags are to be updated
     * @param tagIds  the ids of the tags to associate with the data table
     */
    void updateTagIds(long tableId, List<Long> tagIds);
}
