/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.search;

import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ApiEndpointSearchResult(Long id, long collectionId, String name, String path)
    implements SearchResult<Long> {

    @Override
    public String description() {
        return path;
    }

    @Override
    public SearchAssetType type() {
        return SearchAssetType.API_ENDPOINT;
    }
}
