/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A collection lives in the environment of its backing deployment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionEnvironmentResolverTest {

    private static final long API_COLLECTION_ID = 5L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;

    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    @Test
    void testCollectionEnvironmentIsTheEnvironmentOfItsBackingDeployment() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(apiCollectionService.fetchApiCollection(API_COLLECTION_ID)).thenReturn(Optional.of(apiCollection));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));

        ApiCollectionEnvironmentResolver resolver = new ApiCollectionEnvironmentResolver(
            apiCollectionService, projectDeploymentService);

        assertThat(resolver.fetchEnvironment(API_COLLECTION_ID)).contains(Environment.PRODUCTION);
    }
}
