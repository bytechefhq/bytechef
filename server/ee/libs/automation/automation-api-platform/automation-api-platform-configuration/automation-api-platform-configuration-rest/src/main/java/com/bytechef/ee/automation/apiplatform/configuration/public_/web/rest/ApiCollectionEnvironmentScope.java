/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Scopes the public API's by-id API collection routes to the request's verified environment, answering an API
 * collection of another environment exactly like a missing one. The read goes through the facade's guarded
 * {@link ApiCollectionFacade#getApiCollection(long)}, and a caller without view rights is answered the same way, as a
 * missing API collection, so neither the environment nor the rights reveal that the API collection exists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ApiCollectionEnvironmentScope {

    private ApiCollectionEnvironmentScope() {
    }

    /**
     * Reads the API collection and requires it to belong to the request's environment.
     *
     * @throws ConfigurationException with {@code API_COLLECTION_NOT_FOUND} when the API collection belongs to another
     *                                environment
     */
    static ApiCollectionDTO getApiCollection(
        ApiCollectionFacade apiCollectionFacade, EnvironmentService environmentService, long id,
        @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionDTO apiCollectionDTO;

        try {
            apiCollectionDTO = apiCollectionFacade.getApiCollection(id);
        } catch (AccessDeniedException accessDeniedException) {
            throw notFound(id);
        }

        requireEnvironment(apiCollectionDTO, resolveEnvironment(environmentService, xEnvironment));

        return apiCollectionDTO;
    }

    /**
     * Reads the endpoint through the facade's guarded {@link ApiCollectionFacade#getApiCollectionEndpoint(long)} and
     * requires its API collection to belong to the request's environment.
     *
     * @throws ConfigurationException with {@code API_COLLECTION_NOT_FOUND} when the endpoint's API collection belongs
     *                                to another environment
     */
    static ApiCollectionEndpointDTO getApiCollectionEndpoint(
        ApiCollectionFacade apiCollectionFacade, EnvironmentService environmentService, long id,
        @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionEndpointDTO apiCollectionEndpointDTO;

        try {
            apiCollectionEndpointDTO = apiCollectionFacade.getApiCollectionEndpoint(id);
        } catch (AccessDeniedException accessDeniedException) {
            throw notFound(id);
        }

        getApiCollection(apiCollectionFacade, environmentService, apiCollectionEndpointDTO.apiCollectionId(),
            xEnvironment);

        return apiCollectionEndpointDTO;
    }

    static Environment resolveEnvironment(
        EnvironmentService environmentService, @Nullable EnvironmentModel xEnvironment) {

        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private static void requireEnvironment(ApiCollectionDTO apiCollectionDTO, Environment environment) {
        if (apiCollectionDTO.environment() != environment) {
            throw notFound(apiCollectionDTO.id());
        }
    }

    private static ConfigurationException notFound(Long id) {
        return new ConfigurationException(
            "API collection %s not found".formatted(id), ApiCollectionErrorType.API_COLLECTION_NOT_FOUND);
    }
}
