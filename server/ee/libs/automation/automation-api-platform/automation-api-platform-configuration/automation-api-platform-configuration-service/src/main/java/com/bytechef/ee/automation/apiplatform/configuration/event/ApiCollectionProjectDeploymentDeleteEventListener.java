/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.event;

import com.bytechef.automation.configuration.listener.ProjectDeploymentDeleteEventListener;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Removes the API collection rows referencing a project deployment before that deployment is deleted, so the deployment
 * of an API collection can be deleted together with the project backing it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionProjectDeploymentDeleteEventListener implements ProjectDeploymentDeleteEventListener {

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionService apiCollectionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionProjectDeploymentDeleteEventListener(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionService apiCollectionService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionService = apiCollectionService;
    }

    @Override
    public void onBeforeDeleteProjectDeployment(long projectDeploymentId) {
        List<ApiCollection> apiCollections = apiCollectionService.getProjectDeploymentApiCollections(
            projectDeploymentId);

        for (ApiCollection apiCollection : apiCollections) {
            List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(
                apiCollection.getId());

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
            }

            apiCollectionService.delete(apiCollection.getId());
        }
    }
}
