/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayProviderService {

    AiGatewayProvider create(AiGatewayProvider provider);

    AiGatewayProvider createConnectedUserProvider(AiGatewayProvider provider, long connectedUserId);

    void delete(long id);

    void disableByConnectedUserId(long connectedUserId);

    Optional<AiGatewayProvider> fetchProviderByConnectedUserIdAndType(long connectedUserId, AiGatewayProviderType type);

    AiGatewayProvider getProvider(long id);

    List<AiGatewayProvider> getProviders(Collection<Long> ids);

    List<AiGatewayProvider> getProviders();

    List<AiGatewayProvider> getEnabledProviders();

    List<AiGatewayProvider> getProvidersByConnectedUserId(long connectedUserId);

    List<AiGatewayProvider> getProvidersByWorkspaceId(long workspaceId);

    AiGatewayProvider update(AiGatewayProvider provider);

    void updateWorkspaceId(long id, @Nullable Long workspaceId);
}
