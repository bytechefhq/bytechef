/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * CRUD for {@link AiGatewayProvider}. A provider carries its owning workspace in its nullable {@code workspace_id}
 * column; the workspace-facing policy layer is {@code WorkspaceAiGatewayProviderService} in automation.
 *
 * @version ee
 */
public interface AiGatewayProviderService {

    AiGatewayProvider create(AiGatewayProvider provider);

    void delete(long id);

    AiGatewayProvider getProvider(long id);

    List<AiGatewayProvider> getProviders(Collection<Long> ids);

    List<AiGatewayProvider> getProviders();

    List<AiGatewayProvider> getEnabledProviders();

    List<AiGatewayProvider> getProvidersByWorkspaceId(long workspaceId);

    AiGatewayProvider update(AiGatewayProvider provider);

    /**
     * Sets the provider's owning workspace, or clears it when {@code workspaceId} is null. Separate from
     * {@link #update(AiGatewayProvider)}, which deliberately copies only the caller-editable fields and must not let a
     * detached provider re-stamp its own ownership.
     */
    void updateWorkspaceId(long id, @Nullable Long workspaceId);
}
