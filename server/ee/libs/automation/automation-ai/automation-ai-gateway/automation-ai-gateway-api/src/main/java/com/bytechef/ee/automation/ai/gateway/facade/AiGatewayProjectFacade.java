/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayProjectFacade {

    AiGatewayProject getProject(long id);

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    AiGatewayProject createInWorkspace(AiGatewayProject project, long workspaceId);

    void delete(long id);

    AiGatewayProject update(AiGatewayProject project);
}
