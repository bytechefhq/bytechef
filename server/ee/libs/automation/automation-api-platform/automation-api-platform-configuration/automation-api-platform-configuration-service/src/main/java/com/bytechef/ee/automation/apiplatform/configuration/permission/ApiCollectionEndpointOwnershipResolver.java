/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Maps an API-collection endpoint id to its owning workspace through the collection the stored endpoint belongs to,
 * then the same path {@link ApiCollectionOwnershipResolver} takes. Reads the endpoint from the repository, so the
 * collection id is the persisted one rather than one a request supplied. Fails closed for an unknown endpoint.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionEndpointOwnershipResolver implements ResourceOwnershipResolver {

    private final ApiCollectionOwnershipResolver apiCollectionOwnershipResolver;
    private final ApiEndpointRepository apiEndpointRepository;

    @SuppressFBWarnings("EI")
    public ApiCollectionEndpointOwnershipResolver(
        ApiCollectionOwnershipResolver apiCollectionOwnershipResolver, ApiEndpointRepository apiEndpointRepository) {

        this.apiCollectionOwnershipResolver = apiCollectionOwnershipResolver;
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Override
    public String resourceType() {
        return "ApiCollectionEndpoint";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return apiEndpointRepository.findById(id)
            .map(ApiCollectionEndpoint::getApiCollectionId)
            .filter(Objects::nonNull)
            .map(apiCollectionId -> apiCollectionOwnershipResolver.resolveOwner(apiCollectionId.longValue()))
            .orElseGet(ResourceOwner::unknown);
    }
}
