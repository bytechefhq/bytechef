/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * An endpoint is owned by, and lives in the environment of, the collection its stored row names. The collection id a
 * request supplies is never consulted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionEndpointResolverTest {

    private static final long API_COLLECTION_ID = 5L;
    private static final long ENDPOINT_ID = 9L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long WORKSPACE_ID = 42L;

    private final ApiCollectionOwnershipResolver apiCollectionOwnershipResolver =
        mock(ApiCollectionOwnershipResolver.class);
    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ApiEndpointRepository apiEndpointRepository = mock(ApiEndpointRepository.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    @Test
    void testOwnerIsTheWorkspaceOfTheStoredEndpointsCollection() {
        givenStoredEndpoint();

        when(apiCollectionOwnershipResolver.resolveOwner(API_COLLECTION_ID))
            .thenReturn(ResourceOwner.ofWorkspace(WORKSPACE_ID));

        ApiCollectionEndpointOwnershipResolver resolver = new ApiCollectionEndpointOwnershipResolver(
            apiCollectionOwnershipResolver, apiEndpointRepository);

        assertThat(resolver.resolveOwner(ENDPOINT_ID)
            .workspaceId()).hasValue(WORKSPACE_ID);
    }

    @Test
    void testAnUnknownEndpointHasNoOwner() {
        when(apiEndpointRepository.findById(ENDPOINT_ID)).thenReturn(Optional.empty());

        ApiCollectionEndpointOwnershipResolver resolver = new ApiCollectionEndpointOwnershipResolver(
            apiCollectionOwnershipResolver, apiEndpointRepository);

        assertThat(resolver.resolveOwner(ENDPOINT_ID)
            .workspaceId()).isEmpty();

        verify(apiCollectionOwnershipResolver, never()).resolveOwner(API_COLLECTION_ID);
    }

    @Test
    void testCollectionEnvironmentIsTheEnvironmentOfItsBackingDeployment() {
        givenCollectionDeployedTo(Environment.PRODUCTION);

        ApiCollectionEnvironmentResolver resolver = new ApiCollectionEnvironmentResolver(
            apiCollectionService, projectDeploymentService);

        assertThat(resolver.fetchEnvironment(API_COLLECTION_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testEndpointEnvironmentIsTheEnvironmentOfItsStoredCollection() {
        givenStoredEndpoint();
        givenCollectionDeployedTo(Environment.STAGING);

        ApiCollectionEndpointEnvironmentResolver resolver = new ApiCollectionEndpointEnvironmentResolver(
            new ApiCollectionEnvironmentResolver(apiCollectionService, projectDeploymentService),
            apiEndpointRepository);

        assertThat(resolver.fetchEnvironment(ENDPOINT_ID)).contains(Environment.STAGING);
    }

    private void givenStoredEndpoint() {
        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(API_COLLECTION_ID);

        when(apiEndpointRepository.findById(ENDPOINT_ID)).thenReturn(Optional.of(apiCollectionEndpoint));
    }

    private void givenCollectionDeployedTo(Environment environment) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);

        when(apiCollectionService.fetchApiCollection(API_COLLECTION_ID)).thenReturn(Optional.of(apiCollection));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
    }
}
