/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiGatewayProjectService {

    AiGatewayProject create(AiGatewayProject project);

    void delete(long id);

    Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug);

    AiGatewayProject getProject(long id);

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    AiGatewayProject update(AiGatewayProject project);
}
