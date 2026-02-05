/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.search;

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class ApiCollectionSearchAssetProvider implements SearchAssetProvider {

    private final ApiCollectionService apiCollectionService;

    ApiCollectionSearchAssetProvider(ApiCollectionService apiCollectionService) {
        this.apiCollectionService = apiCollectionService;
    }

    @Override
    public List<ApiCollectionSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        return apiCollectionService.getApiCollections(null, null, null, null)
            .stream()
            .filter(
                apiCollection -> containsIgnoreCase(apiCollection.getName(), queryLower) ||
                    containsIgnoreCase(apiCollection.getDescription(), queryLower))
            .limit(limit)
            .map(
                apiCollection -> new ApiCollectionSearchResult(
                    apiCollection.getId(), apiCollection.getName(), apiCollection.getDescription()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.API_COLLECTION;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
