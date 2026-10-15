/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reports the environment of an API collection as the environment of the project deployment that backs it, the same row
 * {@link ApiCollectionOwnershipResolver} resolves the workspace from.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ApiCollectionEnvironmentResolver(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public String resourceType() {
        return "ApiCollection";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return apiCollectionService.fetchApiCollection(number.longValue())
            .map(ApiCollection::getProjectDeploymentId)
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getEnvironment);
    }
}
