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
 * @version ee
 *
 * @author Ivica Cardic
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

    void updateWorkspaceId(long id, @Nullable Long workspaceId);
}
