/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for {@link AiGatewayProject}. A project carries its owning workspace in its nullable {@code workspace_id}
 * column; the workspace-facing policy layer is {@code WorkspaceAiGatewayProjectService} in automation.
 *
 * @version ee
 */
public interface AiGatewayProjectService {

    AiGatewayProject create(AiGatewayProject project);

    void delete(long id);

    Optional<AiGatewayProject> fetchProject(long id);

    Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug);

    List<AiGatewayProject> getProjects();

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    AiGatewayProject getProject(long id);

    AiGatewayProject update(AiGatewayProject project);
}
