/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * CRUD for {@link AiGatewayRoutingPolicy}. A policy carries its owning workspace in its nullable {@code workspace_id}
 * column; the workspace-facing policy layer is {@code WorkspaceAiGatewayRoutingPolicyService} in automation.
 *
 * @version ee
 */
public interface AiGatewayRoutingPolicyService {

    AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy);

    void delete(long id);

    AiGatewayRoutingPolicy getRoutingPolicy(long id);

    AiGatewayRoutingPolicy getRoutingPolicyByName(String name);

    List<AiGatewayRoutingPolicy> getRoutingPolicies();

    List<AiGatewayRoutingPolicy> getRoutingPolicies(Collection<Long> ids);

    List<AiGatewayRoutingPolicy> getRoutingPoliciesByWorkspaceId(long workspaceId);

    AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy);

    /**
     * Sets the policy's owning workspace, or clears it when {@code workspaceId} is null. Separate from
     * {@link #update(AiGatewayRoutingPolicy)}, which deliberately copies only the caller-editable fields and must not
     * let a detached policy re-stamp its own ownership.
     */
    void updateWorkspaceId(long id, @Nullable Long workspaceId);
}
