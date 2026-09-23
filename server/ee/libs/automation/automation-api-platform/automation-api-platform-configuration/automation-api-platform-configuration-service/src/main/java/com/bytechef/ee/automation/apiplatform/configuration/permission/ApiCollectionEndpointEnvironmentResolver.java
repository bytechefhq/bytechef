/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reports the environment of an API-collection endpoint as the environment of the collection the stored endpoint
 * belongs to.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionEndpointEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ApiCollectionEnvironmentResolver apiCollectionEnvironmentResolver;
    private final ApiEndpointRepository apiEndpointRepository;

    @SuppressFBWarnings("EI")
    public ApiCollectionEndpointEnvironmentResolver(
        ApiCollectionEnvironmentResolver apiCollectionEnvironmentResolver,
        ApiEndpointRepository apiEndpointRepository) {

        this.apiCollectionEnvironmentResolver = apiCollectionEnvironmentResolver;
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Override
    public String resourceType() {
        return "ApiCollectionEndpoint";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return apiEndpointRepository.findById(number.longValue())
            .map(ApiCollectionEndpoint::getApiCollectionId)
            .filter(Objects::nonNull)
            .flatMap(apiCollectionEnvironmentResolver::fetchEnvironment);
    }
}
