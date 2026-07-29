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
 * Service interface for the workspace scoping of AI LLM Gateway providers. A provider belongs to at most one workspace,
 * recorded in its own nullable {@code workspace_id} column.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProviderService {

    /**
     * Gets AI LLM Gateway providers filtered by workspace ID.
     *
     * @param workspaceId the workspace ID to filter by
     * @return a list of AI LLM Gateway providers in the specified workspace
     */
    List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId);

    /**
     * Assigns an AI LLM Gateway provider to a workspace.
     *
     * @param providerId  the provider ID
     * @param workspaceId the workspace ID
     */
    void assignProviderToWorkspace(Long providerId, Long workspaceId);

    /**
     * Removes an AI LLM Gateway provider from its workspace.
     *
     * @param providerId the provider ID
     */
    void removeProviderFromWorkspace(Long providerId);
}
