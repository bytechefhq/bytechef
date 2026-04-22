/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayProjectFacade}. Delegates to the shared services and carries the authorization
 * guards so they are enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayProjectFacadeImpl implements AiGatewayProjectFacade {

    private final AiGatewayProjectService aiGatewayProjectService;
    private final WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;

    @SuppressFBWarnings("EI")
    AiGatewayProjectFacadeImpl(
        AiGatewayProjectService aiGatewayProjectService,
        WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService) {

        this.aiGatewayProjectService = aiGatewayProjectService;
        this.workspaceAiGatewayProjectService = workspaceAiGatewayProjectService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayProject getProject(long id) {
        return aiGatewayProjectService.getProject(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayProjectService.getProjectsByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId) {
        return workspaceAiGatewayProjectService.createInWorkspace(project, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiGatewayProjectService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProject update(AiGatewayProject project) {
        return aiGatewayProjectService.update(project);
    }
}
