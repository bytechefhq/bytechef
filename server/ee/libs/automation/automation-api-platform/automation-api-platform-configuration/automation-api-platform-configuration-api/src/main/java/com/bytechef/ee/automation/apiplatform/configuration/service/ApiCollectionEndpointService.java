/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionEndpointService {

    ApiCollectionEndpoint create(ApiCollectionEndpoint apiCollectionEndpoint);

    void delete(long id);

    Optional<ApiCollectionEndpoint> fetchApiCollectionEndpoint(long id);

    ApiCollectionEndpoint getOpenApiEndpoint(long id);

    List<ApiCollectionEndpoint> getApiEndpoints(long apiCollectionId);

    ApiCollectionEndpoint update(ApiCollectionEndpoint apiCollectionEndpoint);
}
