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

package com.bytechef.automation.data.table.search;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class DataTableSearchAssetProvider implements SearchAssetProvider {

    private final DataTableService dataTableService;
    private final WorkspaceDataTableService workspaceDataTableService;

    DataTableSearchAssetProvider(
        DataTableService dataTableService, WorkspaceDataTableService workspaceDataTableService) {

        this.dataTableService = dataTableService;
        this.workspaceDataTableService = workspaceDataTableService;
    }

    @Override
    public List<DataTableSearchResult> search(String query, int limit, Set<Long> workspaceIds) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        Set<Long> dataTableIds = workspaceIds.stream()
            .flatMap(workspaceId -> workspaceDataTableService.getWorkspaceDataTables(workspaceId)
                .stream())
            .map(WorkspaceDataTable::getDataTableId)
            .collect(Collectors.toSet());

        if (dataTableIds.isEmpty()) {
            return List.of();
        }

        return dataTableService.listTables(1L)
            .stream()
            .filter(table -> dataTableIds.contains(table.id()))
            .filter(table -> containsIgnoreCase(table.baseName(), queryLower))
            .limit(limit)
            .map(table -> new DataTableSearchResult(table.id(), table.baseName()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.DATA_TABLE;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
