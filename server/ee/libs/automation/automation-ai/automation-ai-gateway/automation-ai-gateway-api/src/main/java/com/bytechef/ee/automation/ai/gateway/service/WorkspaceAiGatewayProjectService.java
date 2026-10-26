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
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProjectService {

    AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId);

    Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug);

    Map<Long, Long> getProjectWorkspaceMap();

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    Long getWorkspaceId(long projectId);
}
