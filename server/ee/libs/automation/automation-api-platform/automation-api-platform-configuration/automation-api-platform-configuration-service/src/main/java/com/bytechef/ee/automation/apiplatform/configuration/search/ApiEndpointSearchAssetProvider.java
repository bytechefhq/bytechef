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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private final ApiPlatformWorkspaceResolver apiPlatformWorkspaceResolver;

    @SuppressFBWarnings("EI")
    ApiEndpointSearchAssetProvider(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionService apiCollectionService,
        ApiPlatformWorkspaceResolver apiPlatformWorkspaceResolver) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionService = apiCollectionService;
        this.apiPlatformWorkspaceResolver = apiPlatformWorkspaceResolver;
    }

    @Override
    public List<ApiEndpointSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(null, null, null, null);

        if (apiCollections.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> workspaceIdByProjectDeploymentId =
            apiPlatformWorkspaceResolver.getWorkspaceIdsByProjectDeploymentId(
                apiCollections.stream()
                    .map(ApiCollection::getProjectDeploymentId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());

        List<ApiEndpointSearchResult> results = new ArrayList<>();

        for (ApiCollection apiCollection : apiCollections) {
            List<ApiCollectionEndpoint> endpoints = apiCollectionEndpointService.getApiEndpoints(apiCollection.getId());
            Long workspaceId = workspaceIdByProjectDeploymentId.get(apiCollection.getProjectDeploymentId());

            for (ApiCollectionEndpoint endpoint : endpoints) {
                if (containsIgnoreCase(endpoint.getName(), queryLower) ||
                    containsIgnoreCase(endpoint.getPath(), queryLower)) {

                    results.add(
                        new ApiEndpointSearchResult(
                            endpoint.getId(),
                            apiCollection.getId(),
                            endpoint.getName(),
                            endpoint.getPath(),
                            workspaceId));

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
