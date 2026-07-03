/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

/**
 * Workspace-scoped operations for {@code AiHubPersonalAgent}. The agent entity itself is workspace-agnostic and lives
 * on the platform side; the (workspace, agent) link lives on {@link WorkspaceAiHubPersonalAgent} in this module. This
 * service is the authoritative path for workspace lookups by agent id — callers that need the workspace owning a given
 * agent (notably the GraphQL resolver for {@code AiHubPersonalAgent.workspaceId}) consult this rather than reaching
 * into the platform-side service.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiHubPersonalAgentService {

    /**
     * Returns the workspace id that owns the given agent, looked up via the {@link WorkspaceAiHubPersonalAgent}
     * relation. Throws {@code com.bytechef.ee.ai.hub.exception.NotFoundException} when no membership row exists — an
     * orphan agent is unreachable through every other path, so we surface that data-integrity state loudly rather than
     * silently returning a sentinel.
     */
    long getWorkspaceId(long agentId);
}
