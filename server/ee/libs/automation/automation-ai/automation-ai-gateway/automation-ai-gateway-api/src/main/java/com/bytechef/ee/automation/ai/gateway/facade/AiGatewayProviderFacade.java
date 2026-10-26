/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayProviderFacade {

    AiGatewayProvider getProvider(long id);

    List<AiGatewayProvider> getProviders();

    AiGatewayProvider create(AiGatewayProvider provider);

    void delete(long id);

    AiGatewayProvider update(AiGatewayProvider provider);
}
