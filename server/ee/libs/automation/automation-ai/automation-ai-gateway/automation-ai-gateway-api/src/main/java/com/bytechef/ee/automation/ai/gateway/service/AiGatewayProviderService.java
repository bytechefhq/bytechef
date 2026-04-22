/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import java.util.Collection;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayProviderService {

    AiGatewayProvider create(AiGatewayProvider provider);

    void delete(long id);

    AiGatewayProvider getProvider(long id);

    List<AiGatewayProvider> getProviders(Collection<Long> ids);

    List<AiGatewayProvider> getProviders();

    List<AiGatewayProvider> getEnabledProviders();

    AiGatewayProvider update(AiGatewayProvider provider);
}
