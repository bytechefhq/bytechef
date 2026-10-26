/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.security;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
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
@Component("promotionAuthorizer")
@ConditionalOnEEVersion
public class PromotionAuthorizer {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public PromotionAuthorizer(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    public long projectIdOfApiCollection(long apiCollectionId) {
        ApiCollection apiCollection = apiCollectionService.fetchApiCollection(apiCollectionId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }

    public long workspaceIdOfMcpServer(long mcpServerId) {
        return workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServerId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));
    }

    public long projectIdOfProjectDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}
