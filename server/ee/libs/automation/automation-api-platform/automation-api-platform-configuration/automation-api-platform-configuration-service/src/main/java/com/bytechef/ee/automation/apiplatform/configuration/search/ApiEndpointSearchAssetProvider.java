/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.search;

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class ApiEndpointSearchAssetProvider implements SearchAssetProvider {

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionService apiCollectionService;

    ApiEndpointSearchAssetProvider(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionService apiCollectionService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionService = apiCollectionService;
    }

    @Override
    public List<ApiEndpointSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(null, null, null, null);

        if (apiCollections.isEmpty()) {
            return List.of();
        }

        List<ApiEndpointSearchResult> results = new ArrayList<>();

        for (ApiCollection apiCollection : apiCollections) {
            List<ApiCollectionEndpoint> endpoints = apiCollectionEndpointService.getApiEndpoints(apiCollection.getId());

            for (ApiCollectionEndpoint endpoint : endpoints) {
                if (containsIgnoreCase(endpoint.getName(), queryLower) ||
                    containsIgnoreCase(endpoint.getPath(), queryLower)) {

                    results.add(
                        new ApiEndpointSearchResult(
                            endpoint.getId(),
                            apiCollection.getId(),
                            endpoint.getName(),
                            endpoint.getPath()));

                    if (results.size() >= limit) {
                        return results;
                    }
                }
            }
        }

        return results;
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.API_ENDPOINT;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
