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

package com.bytechef.automation.search.facade;

import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Facade for searching across all automation entities.
 *
 * @author Ivica Cardic
 */
public interface AutomationSearchFacade {

    /**
     * Search across automation entities matching the given query.
     *
     * @param query the search query string
     * @param limit maximum number of results per category
     * @param types the asset types to search; null or empty searches every type
     * @return list of search results
     */
    List<SearchResult<?>> search(String query, int limit, @Nullable Set<SearchAssetType> types);
}
