/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionEndpointService {

    ApiCollectionEndpoint create(@NonNull ApiCollectionEndpoint apiCollectionEndpoint);

    void delete(long id);

    ApiCollectionEndpoint getOpenApiEndpoint(long id);

    List<ApiCollectionEndpoint> getApiEndpoints(long apiCollectionId);

    ApiCollectionEndpoint update(@NonNull ApiCollectionEndpoint apiCollectionEndpoint);
}
