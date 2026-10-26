/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProviderService {

    List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId);

    void assignProviderToWorkspace(Long providerId, Long workspaceId);

    void removeProviderFromWorkspace(Long providerId);
}
