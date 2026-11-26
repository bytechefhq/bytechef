/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.promotion.security;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("apiCollectionPromotionAuthorizer")
@ConditionalOnEEVersion
public class ApiCollectionPromotionAuthorizer {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ApiCollectionPromotionAuthorizer(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
    }

    public long projectIdOfApiCollection(long apiCollectionId) {
        ApiCollection apiCollection = apiCollectionService.fetchApiCollection(apiCollectionId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}
