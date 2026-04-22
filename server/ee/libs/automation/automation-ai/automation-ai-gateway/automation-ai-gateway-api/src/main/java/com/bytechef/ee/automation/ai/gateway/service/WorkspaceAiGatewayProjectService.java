/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 */
public interface WorkspaceAiGatewayProjectService {

    AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId);

    Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug);

    /**
     * Returns the workspace-id of every project. Used by cleanup paths that need to MAX retention per workspace across
     * all projects without N+1 lookups.
     */
    Map<Long, Long> getProjectWorkspaceMap();

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    Long getWorkspaceId(long projectId);
}
