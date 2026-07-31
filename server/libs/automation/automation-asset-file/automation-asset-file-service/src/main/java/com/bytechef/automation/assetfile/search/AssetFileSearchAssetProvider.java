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

package com.bytechef.automation.assetfile.search;

import com.bytechef.automation.assetfile.service.AssetFileService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class AssetFileSearchAssetProvider implements SearchAssetProvider {

    private final AssetFileService assetFileService;

    AssetFileSearchAssetProvider(AssetFileService assetFileService) {
        this.assetFileService = assetFileService;
    }

    @Override
    public List<AssetFileSearchResult> search(String query, int limit) {
        // The query spans every workspace and environment of the current tenant; each result carries its real owning
        // workspaceId so AutomationSearchFacade can drop hits from workspaces the caller is not a member of. The
        // access decision deliberately lives in the facade (which resolves the caller's memberships on the
        // authenticated request thread) because this provider runs on a fan-out pool with no SecurityContext.
        return assetFileService.searchByName(query, limit)
            .stream()
            .map(assetFile -> new AssetFileSearchResult(
                assetFile.getId(), assetFile.getName(), assetFile.getDescription(), assetFile.getWorkspaceId()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.ASSET_FILE;
    }
}
