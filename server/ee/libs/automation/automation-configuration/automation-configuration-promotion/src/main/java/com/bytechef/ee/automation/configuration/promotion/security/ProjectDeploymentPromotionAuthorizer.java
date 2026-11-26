/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.security;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
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
@Component("projectDeploymentPromotionAuthorizer")
@ConditionalOnEEVersion
public class ProjectDeploymentPromotionAuthorizer {

    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentPromotionAuthorizer(ProjectDeploymentService projectDeploymentService) {
        this.projectDeploymentService = projectDeploymentService;
    }

    public long projectIdOfProjectDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}
