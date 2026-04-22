/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;

/**
 * Workspace-agnostic CRUD for {@link AiGatewayProject}. Workspace association + by-workspace lookups live on
 * {@code WorkspaceAiGatewayProjectService} in automation.
 *
 * @version ee
 */
public interface AiGatewayProjectService {

    AiGatewayProject create(AiGatewayProject project);

    void delete(long id);

    List<AiGatewayProject> getProjects();

    AiGatewayProject getProject(long id);

    AiGatewayProject update(AiGatewayProject project);
}
