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

package com.bytechef.automation.workspacefile.search;

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.workspacefile.service.WorkspaceFileService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class WorkspaceFileSearchAssetProvider implements SearchAssetProvider {

    private final WorkspaceFileService workspaceFileService;

    WorkspaceFileSearchAssetProvider(WorkspaceFileService workspaceFileService) {
        this.workspaceFileService = workspaceFileService;
    }

    @Override
    public List<WorkspaceFileSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        return workspaceFileService.findAllByWorkspaceId(1L, null)
            .stream()
            .filter(workspaceFile -> containsIgnoreCase(workspaceFile.getName(), queryLower))
            .limit(limit)
            .map(workspaceFile -> new WorkspaceFileSearchResult(workspaceFile.getId(), workspaceFile.getName()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.WORKSPACE_FILE;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
