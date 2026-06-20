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
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
class ApiCollectionSearchAssetProvider implements SearchAssetProvider {

    private final ApiCollectionService apiCollectionService;
    private final ApiPlatformWorkspaceResolver apiPlatformWorkspaceResolver;

    @SuppressFBWarnings("EI")
    ApiCollectionSearchAssetProvider(
        ApiCollectionService apiCollectionService, ApiPlatformWorkspaceResolver apiPlatformWorkspaceResolver) {

        this.apiCollectionService = apiCollectionService;
        this.apiPlatformWorkspaceResolver = apiPlatformWorkspaceResolver;
    }

    @Override
    public List<ApiCollectionSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(null, null, null, null)
            .stream()
            .filter(
                apiCollection -> containsIgnoreCase(apiCollection.getName(), queryLower) ||
                    containsIgnoreCase(apiCollection.getDescription(), queryLower))
            .limit(limit)
            .toList();

        Map<Long, Long> workspaceIdByProjectDeploymentId =
            apiPlatformWorkspaceResolver.getWorkspaceIdsByProjectDeploymentId(
                apiCollections.stream()
                    .map(ApiCollection::getProjectDeploymentId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());

        return apiCollections.stream()
            .map(
                apiCollection -> new ApiCollectionSearchResult(
                    apiCollection.getId(), apiCollection.getName(), apiCollection.getDescription(),
                    workspaceIdByProjectDeploymentId.get(apiCollection.getProjectDeploymentId())))
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
