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
import java.util.Locale;
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
        String queryLower = query.toLowerCase(Locale.ROOT);

        // Workspace and environment are placeholders here — the SearchAssetProvider contract does not
        // currently thread either through. Tracking parity with the existing hardcoded workspaceId=1L until
        // the search infrastructure carries a request-scoped workspace + environment context.
        return assetFileService.findAllByWorkspaceIdAndEnvironment(1L, 0, null)
            .stream()
            .filter(assetFile -> containsIgnoreCase(assetFile.getName(), queryLower))
            .limit(limit)
            .map(assetFile -> new AssetFileSearchResult(assetFile.getId(), assetFile.getName()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.ASSET_FILE;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
